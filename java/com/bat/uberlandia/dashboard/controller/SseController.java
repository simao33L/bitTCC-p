package com.bat.uberlandia.dashboard.controller;

import com.bat.uberlandia.dashboard.model.Chamado;
import com.bat.uberlandia.dashboard.repository.ChamadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

    private final ChamadoRepository chamadoRepository;
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @GetMapping("/chamado/{id}/tempo")
    public SseEmitter streamTempo(@PathVariable Long id) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(id, emitter);

        emitter.onCompletion(() -> emitters.remove(id));
        emitter.onTimeout(() -> emitters.remove(id));
        emitter.onError(e -> emitters.remove(id));

        scheduler.scheduleAtFixedRate(() -> {
            SseEmitter em = emitters.get(id);
            if (em == null) return;
            try {
                Chamado c = chamadoRepository.findById(id).orElse(null);
                if (c == null) return;
                long segundos = c.getTempoReparoSegundos();
                long minutos = segundos / 60;
                long restoSeg = segundos % 60;
                boolean atrasado = c.isAtrasado();
                String formatted = String.format("%02d:%02d", minutos, restoSeg);

                SseEmitter.SseEventBuilder event = SseEmitter.event()
                        .name("tempo")
                        .data("{\"segundos\":" + segundos
                                + ",\"minutos\":" + (segundos / 60.0)
                                + ",\"formatado\":\"" + formatted + "\""
                                + ",\"atrasado\":" + atrasado
                                + ",\"status\":\"" + c.getStatus().name() + "\"}");
                em.send(event);
            } catch (Exception ex) {
                emitters.remove(id);
            }
        }, 0, 1, TimeUnit.SECONDS);

        return emitter;
    }

    @GetMapping("/dashboard/status")
    public SseEmitter streamDashboard() {
        SseEmitter emitter = new SseEmitter(0L);
        return emitter;
    }
}