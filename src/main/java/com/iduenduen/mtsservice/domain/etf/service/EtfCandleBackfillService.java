package com.iduenduen.mtsservice.domain.etf.service;

import com.iduenduen.mtsservice.common.ls.stock.dto.LsUnifiedDailyCandleResponse;
import com.iduenduen.mtsservice.common.ls.stock.dto.LsUnifiedMinuteCandleResponse;
import com.iduenduen.mtsservice.common.ls.stock.service.LsUnifiedDailyCandleService;
import com.iduenduen.mtsservice.common.ls.stock.service.LsUnifiedMinuteCandleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * LS API(t8451 일/주/월봉, t8452 분봉)로 과거 캔들을 페이지네이션으로 수집한다.
 * 수집(HTTP)과 저장(트랜잭션)을 분리해, 트랜잭션이 외부 API 호출 동안 DB 커넥션을 점유하지 않도록 한다.
 * 실제 DB 저장은 {@link EtfCandlePersistService}에 위임한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EtfCandleBackfillService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String EXCHANGE_KRX = "K";

    private static final String LISTING_FLOOR_DATE = "19560101";
    private static final int MAX_PAGE = 100;
    // LS API 초당 호출 제한 준수: 연속조회 페이지 사이 대기 (ms)
    private static final long PAGE_DELAY_MS = 1000;

    private final LsUnifiedDailyCandleService lsUnifiedDailyCandleService;
    private final LsUnifiedMinuteCandleService lsUnifiedMinuteCandleService;
    private final EtfCandlePersistService etfCandlePersistService;

    public void backfillDaily(Long etfId, String shcode) {
        List<LsUnifiedDailyCandleResponse.T8451OutBlock1> rows =
                collectDailyRows(shcode, LsUnifiedDailyCandleService.GUBUN_DAY);
        etfCandlePersistService.saveDaily(etfId, rows);
    }

    public void backfillWeekly(Long etfId, String shcode) {
        List<LsUnifiedDailyCandleResponse.T8451OutBlock1> rows =
                collectDailyRows(shcode, LsUnifiedDailyCandleService.GUBUN_WEEK);
        etfCandlePersistService.saveWeekly(etfId, rows);
    }

    public void backfillMonthly(Long etfId, String shcode) {
        List<LsUnifiedDailyCandleResponse.T8451OutBlock1> rows =
                collectDailyRows(shcode, LsUnifiedDailyCandleService.GUBUN_MONTH);
        etfCandlePersistService.saveMonthly(etfId, rows);
    }

    public void backfillMinute1m(Long etfId, String shcode, String sdate, String edate) {
        etfCandlePersistService.saveMinute1m(etfId, collectMinuteRows(shcode, 1, sdate, edate));
    }

    public void backfillMinute10m(Long etfId, String shcode, String sdate, String edate) {
        etfCandlePersistService.saveMinute10m(etfId, collectMinuteRows(shcode, 10, sdate, edate));
    }

    public void backfillMinute30m(Long etfId, String shcode, String sdate, String edate) {
        etfCandlePersistService.saveMinute30m(etfId, collectMinuteRows(shcode, 30, sdate, edate));
    }

    public void backfillMinute60m(Long etfId, String shcode, String sdate, String edate) {
        etfCandlePersistService.saveMinute60m(etfId, collectMinuteRows(shcode, 60, sdate, edate));
    }

    // 상장일(혹은 그보다 더 이전)부터 오늘까지 전체 구간을 요청해, 거래소 데이터가 존재하는 만큼만 받아온다.
    private List<LsUnifiedDailyCandleResponse.T8451OutBlock1> collectDailyRows(String shcode, String gubun) {
        String today = LocalDate.now().format(DATE_FORMAT);
        List<LsUnifiedDailyCandleResponse.T8451OutBlock1> all = new ArrayList<>();

        LsUnifiedDailyCandleResponse response =
                lsUnifiedDailyCandleService.getUnifiedDailyCandles(shcode, gubun, LISTING_FLOOR_DATE, today, EXCHANGE_KRX);
        addDailyRows(all, response);

        int page = 0;
        while (response.hasNext() && page < MAX_PAGE) {
            throttle();
            String ctsDate = response.getT8451OutBlock().getCts_date();
            response = lsUnifiedDailyCandleService.getUnifiedDailyCandlesContinue(
                    shcode, gubun, LISTING_FLOOR_DATE, today, ctsDate, EXCHANGE_KRX);
            addDailyRows(all, response);
            page++;
        }
        if (response.hasNext()) {
            log.warn("[*] 일/주/월봉 수집이 MAX_PAGE({})에서 잘림. shcode={}, gubun={}, 수집행수={}", MAX_PAGE, shcode, gubun, all.size());
        }
        return all;
    }

    private List<LsUnifiedMinuteCandleResponse.T8452OutBlock1> collectMinuteRows(String shcode, int ncnt, String sdate, String edate) {
        List<LsUnifiedMinuteCandleResponse.T8452OutBlock1> all = new ArrayList<>();

        LsUnifiedMinuteCandleResponse response =
                lsUnifiedMinuteCandleService.getUnifiedMinuteCandles(shcode, ncnt, sdate, edate, EXCHANGE_KRX);
        addMinuteRows(all, response);

        int page = 0;
        while (response.hasNext() && page < MAX_PAGE) {
            throttle();
            String ctsDate = response.getT8452OutBlock().getCts_date();
            String ctsTime = response.getT8452OutBlock().getCts_time();
            response = lsUnifiedMinuteCandleService.getUnifiedMinuteCandlesContinue(
                    shcode, ncnt, sdate, edate, ctsDate, ctsTime, EXCHANGE_KRX);
            addMinuteRows(all, response);
            page++;
        }
        if (response.hasNext()) {
            log.warn("[*] {}분봉 수집이 MAX_PAGE({})에서 잘림. shcode={}, 수집행수={}", ncnt, MAX_PAGE, shcode, all.size());
        }
        return all;
    }

    private void addDailyRows(List<LsUnifiedDailyCandleResponse.T8451OutBlock1> target, LsUnifiedDailyCandleResponse response) {
        List<LsUnifiedDailyCandleResponse.T8451OutBlock1> rows = response.getT8451OutBlock1();
        if (rows != null) {
            target.addAll(rows);
        }
    }

    private void addMinuteRows(List<LsUnifiedMinuteCandleResponse.T8452OutBlock1> target, LsUnifiedMinuteCandleResponse response) {
        List<LsUnifiedMinuteCandleResponse.T8452OutBlock1> rows = response.getT8452OutBlock1();
        if (rows != null) {
            target.addAll(rows);
        }
    }

    private void throttle() {
        try {
            Thread.sleep(PAGE_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
