package com.samaheim.world;

import java.util.List;

/** Small deterministic runtime physics layer for the current playable client. */
public final class SandboxPhysics {
    private static final float EPS = 0.0001f;
    private static final float MAX_HORIZONTAL_STEP = 0.30f;
    private static final int MAX_DEPENETRATION_PASSES = 5;

    private SandboxPhysics() {}

    public record CircleBlocker(float x, float z, float radius) {}
    public record BoxBlocker(float x, float z, float halfX, float halfZ, float yawRadians) {}
    public record HorizontalResult(float x, float z, boolean blocked) {}
    public record VerticalResult(float eyeY, float velocityY, boolean grounded) {}

    /** Backward-compatible circle-only collision path. */
    public static HorizontalResult resolveHorizontal(float fromX, float fromZ, float toX, float toZ,
                                                     float playerRadius, List<CircleBlocker> blockers) {
        return resolveHorizontalMixed(fromX, fromZ, toX, toZ, playerRadius, blockers, List.of());
    }

    /** Swept collision against natural circular blockers and oriented rectangular structures. */
    public static HorizontalResult resolveHorizontalMixed(float fromX, float fromZ, float toX, float toZ,
                                                          float playerRadius, List<CircleBlocker> circles,
                                                          List<BoxBlocker> boxes) {
        if (!Float.isFinite(fromX) || !Float.isFinite(fromZ) || !Float.isFinite(toX) || !Float.isFinite(toZ)) {
            return new HorizontalResult(fromX, fromZ, true);
        }
        List<CircleBlocker> safeCircles = circles == null ? List.of() : circles;
        List<BoxBlocker> safeBoxes = boxes == null ? List.of() : boxes;
        float dxTotal = toX - fromX;
        float dzTotal = toZ - fromZ;
        float distance = (float) Math.sqrt(dxTotal * dxTotal + dzTotal * dzTotal);
        int steps = Math.max(1, (int) Math.ceil(distance / MAX_HORIZONTAL_STEP));
        float stepX = dxTotal / steps;
        float stepZ = dzTotal / steps;
        float x = fromX;
        float z = fromZ;
        boolean blocked = false;

        for (int step = 0; step < steps; step++) {
            float candidateX = x + stepX;
            float candidateZ = z + stepZ;
            Depenetration dep = depenetrate(candidateX, candidateZ, x, z,
                    Math.max(0f, playerRadius), safeCircles, safeBoxes);
            x = dep.x;
            z = dep.z;
            blocked |= dep.blocked;
        }
        return new HorizontalResult(x, z, blocked);
    }

    private static Depenetration depenetrate(float candidateX, float candidateZ, float fallbackX, float fallbackZ,
                                             float playerRadius, List<CircleBlocker> circles, List<BoxBlocker> boxes) {
        float x = candidateX;
        float z = candidateZ;
        boolean blocked = false;
        for (int pass = 0; pass < MAX_DEPENETRATION_PASSES; pass++) {
            boolean changed = false;
            for (CircleBlocker blocker : circles) {
                if (blocker == null || !finite(blocker.x(), blocker.z(), blocker.radius())) continue;
                float minDistance = playerRadius + Math.max(0f, blocker.radius());
                if (minDistance <= EPS) continue;
                float dx = x - blocker.x();
                float dz = z - blocker.z();
                float distanceSq = dx * dx + dz * dz;
                if (distanceSq >= minDistance * minDistance - EPS) continue;
                blocked = true;
                changed = true;
                float distance = (float) Math.sqrt(Math.max(0f, distanceSq));
                if (distance <= EPS) {
                    dx = fallbackX - blocker.x();
                    dz = fallbackZ - blocker.z();
                    distance = (float) Math.sqrt(dx * dx + dz * dz);
                    if (distance <= EPS) { dx = 1f; dz = 0f; distance = 1f; }
                }
                float push = (minDistance + 0.001f) / distance;
                x = blocker.x() + dx * push;
                z = blocker.z() + dz * push;
            }
            for (BoxBlocker blocker : boxes) {
                BoxPush push = pushOutOfBox(x, z, fallbackX, fallbackZ, playerRadius, blocker);
                if (!push.blocked) continue;
                blocked = true;
                changed = true;
                x = push.x;
                z = push.z;
            }
            if (!changed) break;
        }
        return new Depenetration(x, z, blocked);
    }

    private static BoxPush pushOutOfBox(float worldX, float worldZ, float fallbackX, float fallbackZ,
                                        float radius, BoxBlocker box) {
        if (box == null || !finite(box.x(), box.z(), box.halfX(), box.halfZ(), box.yawRadians())) {
            return new BoxPush(worldX, worldZ, false);
        }
        float hx = Math.max(0f, box.halfX());
        float hz = Math.max(0f, box.halfZ());
        if (hx <= EPS || hz <= EPS) return new BoxPush(worldX, worldZ, false);
        float cos = (float) Math.cos(box.yawRadians());
        float sin = (float) Math.sin(box.yawRadians());
        float dx = worldX - box.x();
        float dz = worldZ - box.z();
        float lx = cos * dx + sin * dz;
        float lz = -sin * dx + cos * dz;
        float ex = hx + radius;
        float ez = hz + radius;
        if (Math.abs(lx) >= ex - EPS || Math.abs(lz) >= ez - EPS) return new BoxPush(worldX, worldZ, false);

        float penX = ex - Math.abs(lx);
        float penZ = ez - Math.abs(lz);
        if (penX < penZ) {
            float sign = Math.signum(lx);
            if (sign == 0f) sign = fallbackLocalSign(fallbackX, fallbackZ, box, true);
            lx = sign * (ex + 0.001f);
        } else {
            float sign = Math.signum(lz);
            if (sign == 0f) sign = fallbackLocalSign(fallbackX, fallbackZ, box, false);
            lz = sign * (ez + 0.001f);
        }
        float wx = box.x() + cos * lx - sin * lz;
        float wz = box.z() + sin * lx + cos * lz;
        return new BoxPush(wx, wz, true);
    }

    private static float fallbackLocalSign(float fallbackX, float fallbackZ, BoxBlocker box, boolean xAxis) {
        float cos = (float) Math.cos(box.yawRadians());
        float sin = (float) Math.sin(box.yawRadians());
        float dx = fallbackX - box.x();
        float dz = fallbackZ - box.z();
        float local = xAxis ? cos * dx + sin * dz : -sin * dx + cos * dz;
        return local < 0f ? -1f : 1f;
    }

    /** Stable vertical integration against the edited/support heightfield. */
    public static VerticalResult stepVertical(float currentEyeY, float velocityY, float groundY, float eyeHeight,
                                              boolean jumpRequested, float dt) {
        if (!Float.isFinite(currentEyeY) || !Float.isFinite(velocityY) || !Float.isFinite(groundY)
                || !Float.isFinite(eyeHeight) || !Float.isFinite(dt)) {
            float safeGround = Float.isFinite(groundY) ? groundY : 0f;
            float safeHeight = Float.isFinite(eyeHeight) ? Math.max(0f, eyeHeight) : 1.72f;
            return new VerticalResult(safeGround + safeHeight, 0f, true);
        }
        float frame = Math.max(0f, Math.min(dt, 0.05f));
        float floorEye = groundY + Math.max(0f, eyeHeight);
        boolean grounded = currentEyeY <= floorEye + 0.055f && velocityY <= 0.15f;
        float nextVelocity = velocityY;
        if (grounded) {
            if (jumpRequested) {
                currentEyeY = Math.max(currentEyeY, floorEye);
                nextVelocity = 6.2f;
                grounded = false;
            } else {
                currentEyeY = floorEye;
                nextVelocity = 0f;
            }
        }
        if (!grounded) nextVelocity -= 18.5f * frame;
        float nextY = currentEyeY + nextVelocity * frame;
        if (nextY <= floorEye) {
            nextY = floorEye;
            nextVelocity = 0f;
            grounded = true;
        }
        return new VerticalResult(nextY, nextVelocity, grounded);
    }

    private static boolean finite(float... values) {
        for (float value : values) if (!Float.isFinite(value)) return false;
        return true;
    }

    private record Depenetration(float x, float z, boolean blocked) {}
    private record BoxPush(float x, float z, boolean blocked) {}
}
