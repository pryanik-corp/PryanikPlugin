package com.example.pryanik;

import java.util.UUID;

public record Clan(UUID id, String name, UUID leader, String homeWorld, double homeX, double homeY, double homeZ, float homeYaw, float homePitch) {
    public boolean hasHome() { return homeWorld != null && !homeWorld.isBlank(); }
}
