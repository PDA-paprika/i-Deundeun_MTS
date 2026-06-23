#!/bin/bash

set -e

DOCKER_USERNAME=$1
IMAGE_TAG=$2

IMAGE_NAME="$DOCKER_USERNAME/ideundeun-etf:$IMAGE_TAG"

BLUE_NAME="etf-blue"
GREEN_NAME="etf-green"

BLUE_PORT=8083
GREEN_PORT=8084

NGINX_CONF="/etc/nginx/conf.d/etf.conf"

echo "ETF/MTS Server Blue/Green 배포 시작"
echo "IMAGE_NAME=$IMAGE_NAME"

docker pull $IMAGE_NAME

if grep -q "127.0.0.1:${BLUE_PORT}" $NGINX_CONF; then
  CURRENT_NAME=$BLUE_NAME
  CURRENT_PORT=$BLUE_PORT
  NEXT_NAME=$GREEN_NAME
  NEXT_PORT=$GREEN_PORT
  COMPOSE_FILE="docker-compose.green.yml"
else
  CURRENT_NAME=$GREEN_NAME
  CURRENT_PORT=$GREEN_PORT
  NEXT_NAME=$BLUE_NAME
  NEXT_PORT=$BLUE_PORT
  COMPOSE_FILE="docker-compose.blue.yml"
fi

echo "현재 운영 컨테이너: $CURRENT_NAME / port $CURRENT_PORT"
echo "새 배포 대상: $NEXT_NAME / port $NEXT_PORT"

docker rm -f $NEXT_NAME || true

DOCKER_USERNAME=$DOCKER_USERNAME IMAGE_TAG=$IMAGE_TAG docker-compose -f $COMPOSE_FILE up -d

echo "Health check 시작"

for i in {1..40}; do
  if curl -f http://localhost:$NEXT_PORT/actuator/health; then
    echo "Health check 성공"
    break
  fi

  if [ $i -eq 40 ]; then
    echo "Health check 실패"
    docker logs $NEXT_NAME
    exit 1
  fi

  sleep 3
done

echo "Nginx upstream 전환: $CURRENT_PORT -> $NEXT_PORT"

sudo sed -i "s/127.0.0.1:${CURRENT_PORT}/127.0.0.1:${NEXT_PORT}/g" $NGINX_CONF

sudo nginx -t
sudo systemctl reload nginx

docker rm -f $CURRENT_NAME || true
docker image prune -f

echo "ETF/MTS Server Blue/Green 배포 완료"