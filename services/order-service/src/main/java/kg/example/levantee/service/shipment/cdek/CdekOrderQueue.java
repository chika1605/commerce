package kg.example.levantee.service.shipment.cdek;

import kg.example.levantee.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class CdekOrderQueue {

    private static final String QUEUE_KEY = "cdek:order:queue";

    private final StringRedisTemplate redisTemplate;
    private final ShipmentRepository shipmentRepository;

    @PostConstruct
    public void loadFromDatabase() {
        redisTemplate.delete(QUEUE_KEY);

        var acceptedShipments = shipmentRepository.findAllByCdekRequestStatus("ACCEPTED");
        for (var shipment : acceptedShipments) {
            redisTemplate.opsForList().rightPush(QUEUE_KEY, String.valueOf(shipment.getId()));
        }
        if (!acceptedShipments.isEmpty()) {
            log.info("CdekOrderQueue: загрузили {} ACCEPTED-шипментов из БД в Redis при старте", acceptedShipments.size());
        }
    }

    public void enqueue(Long shipmentId) {
        redisTemplate.opsForList().rightPush(QUEUE_KEY, String.valueOf(shipmentId));
        log.info("CdekOrderQueue: добавили shipmentId={} в Redis-очередь", shipmentId);
    }

    public Long poll() {
        String value = redisTemplate.opsForList().leftPop(QUEUE_KEY);
        if (value == null) return null;
        return Long.parseLong(value);
    }

    public Long size() {
        return redisTemplate.opsForList().size(QUEUE_KEY);
    }
}