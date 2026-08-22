package com.aclg.apecan.shared.audit;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

final class RelogioMutavel extends Clock {

    private Instant instante;
    private final ZoneId zona;

    RelogioMutavel(Instant instante, ZoneId zona) {
        this.instante = Objects.requireNonNull(instante);
        this.zona = Objects.requireNonNull(zona);
    }

    void avancarPara(Instant novoInstante) {
        this.instante = Objects.requireNonNull(novoInstante);
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new RelogioMutavel(instante, zone);
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
