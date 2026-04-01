package kg.example.levantee.service.shipment.cdek;

import kg.example.levantee.model.entity.shipment.Shipment;
import kg.example.levantee.repository.ShipmentRepository;
import kg.example.levantee.service.shipment.cdek.client.CdekClient;
import kg.example.levantee.service.shipment.cdek.model.CdekOrderApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CdekOrderStatusPollerService {

    private static final int MAX_POLL_ATTEMPTS = 3;

    private final CdekClient cdekClient;
    private final ShipmentRepository shipmentRepository;
    private final CdekOrderQueue cdekOrderQueue;

    @Scheduled(fixedDelay = 30_000)
    public void pollAcceptedOrders() {
        Set<Long> shipmentIds = new LinkedHashSet<>();
        Long id;
        while ((id = cdekOrderQueue.poll()) != null) {
            shipmentIds.add(id);
        }

        if (shipmentIds.isEmpty()) return;

        log.info("Поллинг статуса: {} CDEK-заказов из Redis-очереди", shipmentIds.size());

        for (Long shipmentId : shipmentIds) {
            Shipment shipment = shipmentRepository.findById(shipmentId).orElse(null);
            if (shipment == null) {
                log.warn("Shipment #{} не найден в БД, пропускаем", shipmentId);
                continue;
            }

            if (shipment.getCdekPollAttempts() >= MAX_POLL_ATTEMPTS) {
                log.warn("Shipment #{} cdekUuid={} исчерпал {} попыток, сбрасываем счётчик и кладём в конец очереди",
                        shipment.getId(), shipment.getCdekUuid(), MAX_POLL_ATTEMPTS);
                shipment.setCdekPollAttempts(0);
                shipmentRepository.save(shipment);
                cdekOrderQueue.enqueue(shipment.getId());
                continue;
            }

            shipment.setCdekPollAttempts(shipment.getCdekPollAttempts() + 1);

            try {
                CdekOrderApiResponse response = cdekClient.getOrderStatus(shipment.getCdekUuid());
                String newState = extractState(response);

                if ("SUCCESSFUL".equals(newState)) {
                    shipment.setCdekRequestStatus("SUCCESSFUL");
                    shipment.setCdekNumber(response.getEntity().getCdekNumber());
                    shipment.setCdekStatus(response.getEntity().getCurrentStatusName());
                    log.info("Shipment #{} cdekUuid={}: SUCCESSFUL, cdekNumber={}",
                            shipment.getId(), shipment.getCdekUuid(), shipment.getCdekNumber());

                } else if ("INVALID".equals(newState)) {
                    shipment.setCdekRequestStatus("INVALID");
                    logErrors(response, shipment.getCdekUuid());
                    log.error("Shipment #{} cdekUuid={}: INVALID — заказ не создан в CDEK",
                            shipment.getId(), shipment.getCdekUuid());

                } else if ("ACCEPTED".equals(newState)) {
                    log.info("Shipment #{} cdekUuid={}: ACCEPTED — запрос ещё обрабатывается, попытка {}/{}",
                            shipment.getId(), shipment.getCdekUuid(),
                            shipment.getCdekPollAttempts(), MAX_POLL_ATTEMPTS);
                    cdekOrderQueue.enqueue(shipment.getId());

                } else {
                    log.warn("Shipment #{} cdekUuid={}: неизвестный статус '{}', попытка {}/{}, кладём обратно в очередь",
                            shipment.getId(), shipment.getCdekUuid(), newState,
                            shipment.getCdekPollAttempts(), MAX_POLL_ATTEMPTS);
                    cdekOrderQueue.enqueue(shipment.getId());
                }

            } catch (Exception e) {
                log.error("Ошибка при опросе CDEK uuid={}: {}", shipment.getCdekUuid(), e.getMessage());
                cdekOrderQueue.enqueue(shipment.getId());
            }

            shipmentRepository.save(shipment);
        }
    }

    private String extractState(CdekOrderApiResponse response) {
        if (response.getRequests() != null && !response.getRequests().isEmpty()) {
            return response.getRequests().get(0).getState();
        }
        return "UNKNOWN";
    }

    private void logErrors(CdekOrderApiResponse response, String uuid) {
        if (response.getRequests() == null) return;
        response.getRequests().forEach(r -> {
            if (r.getErrors() != null) {
                r.getErrors().forEach(e ->
                        log.error("CDEK ошибка uuid={}: [{}] {}", uuid, e.getCode(), e.getMessage()));
            }
        });
    }
}