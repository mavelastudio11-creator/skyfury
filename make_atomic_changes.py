import re

path = "app/src/main/java/com/mavelastudio/skyfury/SkyFuryGameView.java"
with open(path, "r") as f:
    text = f.read()

# 1. Constants
text = text.replace(
    "private static final int POWER_GUARDIAN = 6;",
    "private static final int POWER_GUARDIAN = 6;\n    private static final int POWER_ATOMIC = 7;"
)

# 2. Resource Array
text = text.replace(
    "R.drawable.guardian_angel",
    "R.drawable.guardian_angel,\n            R.drawable.atomic_bomb"
)

# 3. Fields
text = text.replace(
    "private int specialCharges;",
    "private int specialCharges;\n    private int atomicCharges;\n    private int cachedAtomicCharges = -1;\n    private String atomicChargeText = \"x0\";"
)
text = text.replace(
    "private int specialPointerId = INVALID_POINTER_ID;",
    "private int specialPointerId = INVALID_POINTER_ID;\n    private int atomicPointerId = INVALID_POINTER_ID;"
)

# 4. resetGame()
text = text.replace(
    "specialCharges = 3;",
    "specialCharges = 3;\n        atomicCharges = 0;\n        cachedAtomicCharges = -1;"
)
text = text.replace(
    "specialPointerId = INVALID_POINTER_ID;",
    "specialPointerId = INVALID_POINTER_ID;\n        atomicPointerId = INVALID_POINTER_ID;"
)

# 5. getPowerLabel
text = text.replace(
    "} else if (type == POWER_SPECIAL) {\n            return \"SPECIAL RECHARGE\";\n        }",
    "} else if (type == POWER_SPECIAL) {\n            return \"SPECIAL RECHARGE\";\n        } else if (type == POWER_ATOMIC) {\n            return \"ATOMIC BOMB\";\n        }"
)

# 6. getPowerColor
text = text.replace(
    "} else if (type == POWER_SPECIAL) {\n            return Color.rgb(240, 240, 240);\n        }",
    "} else if (type == POWER_SPECIAL) {\n            return Color.rgb(240, 240, 240);\n        } else if (type == POWER_ATOMIC) {\n            return Color.rgb(255, 120, 0);\n        }"
)

# 7. getRandomPickup
text = text.replace(
    "if (random.nextFloat() < 0.05f) {\n            return POWER_GUARDIAN;\n        }",
    "if (random.nextFloat() < 0.05f) {\n            return POWER_GUARDIAN;\n        }\n        if (wave >= 8 && random.nextFloat() < 0.04f) {\n            return POWER_ATOMIC;\n        }"
)

# 8. applyPickup
text = text.replace(
    "} else if (type == POWER_SPECIAL) {\n            specialCharges++;\n            triggerBanner(\"LASER CHARGE +1!\");\n        }",
    "} else if (type == POWER_SPECIAL) {\n            specialCharges++;\n            triggerBanner(\"LASER CHARGE +1!\");\n        } else if (type == POWER_ATOMIC) {\n            atomicCharges++;\n            triggerBanner(\"ATOMIC BOMB ACQUIRED!\");\n        }"
)

# 9. updateGame (cached values)
text = text.replace(
    "if (specialCharges != cachedSpecialCharges) {",
    "if (atomicCharges != cachedAtomicCharges) {\n            cachedAtomicCharges = atomicCharges;\n            atomicChargeText = \"x\" + atomicCharges;\n        }\n        if (specialCharges != cachedSpecialCharges) {"
)

# 10. isSpecialButtonHit -> add atomic button methods
atomic_methods = """
    private float getAtomicButtonRadius() {
        return getJoystickRadius() * 0.88f;
    }

    private float getAtomicButtonCenterX() {
        return getJoystickRadius() * 1.88f;
    }

    private float getAtomicButtonCenterY() {
        return getJoystickCenterY();
    }

    private boolean isAtomicButtonHit(float x, float y) {
        return distanceSquared(x, y, getAtomicButtonCenterX(), getAtomicButtonCenterY()) <= square(getAtomicButtonRadius() * 1.35f);
    }
"""
text = text.replace(
    "private boolean isSpecialButtonHit(float x, float y) {\n        return distanceSquared(x, y, getSpecialButtonCenterX(), getSpecialButtonCenterY()) <= square(getSpecialButtonRadius() * 1.35f);\n    }",
    "private boolean isSpecialButtonHit(float x, float y) {\n        return distanceSquared(x, y, getSpecialButtonCenterX(), getSpecialButtonCenterY()) <= square(getSpecialButtonRadius() * 1.35f);\n    }\n" + atomic_methods
)

# 11. drawGame -> draw atomic button
draw_atomic = """
        float ar = getAtomicButtonRadius();
        float ax = getAtomicButtonCenterX();
        float ay = getAtomicButtonCenterY();
        boolean aReady = atomicCharges > 0;
        boolean aPressed = atomicPointerId != INVALID_POINTER_ID;
        paint.setColor(aReady ? Color.argb(aPressed ? 220 : 178, 245, 80, 50) : Color.argb(86, 145, 151, 158));
        canvas.drawCircle(ax, ay, aPressed ? ar * 0.94f : ar, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(3f, ar * 0.07f));
        paint.setColor(aReady ? Color.argb(220, 255, 150, 100) : Color.argb(130, 230, 230, 230));
        canvas.drawCircle(ax, ay, ar, paint);
        paint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(boldTypeface);
        textPaint.setTextSize(Math.max(18f, ar * 0.35f));
        textPaint.setColor(aReady ? Color.rgb(47, 10, 0) : Color.argb(180, 40, 43, 47));
        drawIronblockText(canvas, "ATOMIC", ax, ay + ar * 0.06f, textPaint);
        textPaint.setTextSize(Math.max(12f, ar * 0.27f));
        drawIronblockText(canvas, atomicChargeText, ax, ay + ar * 0.54f, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
"""
# inject right before `textPaint.setTextAlign(Paint.Align.LEFT);` in drawSpecialButton code
text = text.replace(
    "drawIronblockText(canvas, specialChargeText, bx, by + br * 0.54f, textPaint);\n        textPaint.setTextAlign(Paint.Align.LEFT);\n    }",
    "drawIronblockText(canvas, specialChargeText, bx, by + br * 0.54f, textPaint);\n" + draw_atomic + "\n    }"
)

# 12. onTouchEvent pointer down
text = text.replace(
    "} else if (isSpecialButtonHit(x, y)) {\n                    specialPointerId = pointerId;\n                }",
    "} else if (isSpecialButtonHit(x, y)) {\n                    specialPointerId = pointerId;\n                } else if (isAtomicButtonHit(x, y)) {\n                    atomicPointerId = pointerId;\n                }"
)

# 13. onTouchEvent pointer up
trigger_atomic = """
        if (pointerId == atomicPointerId) {
            atomicPointerId = INVALID_POINTER_ID;
            if (atomicCharges > 0 && isAtomicButtonHit(x, y)) {
                atomicCharges--;
                triggerAtomicBomb();
            }
        }
"""
text = text.replace(
    "if (pointerId == specialPointerId) {\n            specialPointerId = INVALID_POINTER_ID;\n            if (specialCharges > 0 && laserTimer <= 0f && isSpecialButtonHit(x, y)) {\n                specialCharges--;\n                laserTimer = 5.25f;\n            }\n        }",
    "if (pointerId == specialPointerId) {\n            specialPointerId = INVALID_POINTER_ID;\n            if (specialCharges > 0 && laserTimer <= 0f && isSpecialButtonHit(x, y)) {\n                specialCharges--;\n                laserTimer = 5.25f;\n            }\n        }\n" + trigger_atomic
)

# 14. Add triggerAtomicBomb method
trigger_method = """
    private void triggerAtomicBomb() {
        for (Enemy enemy : enemies) {
            addExplosion(enemy.x, enemy.y, getEnemyRadius(enemy) * 4f, Color.rgb(255, 120, 0));
        }
        enemies.clear();
        enemyShots.clear();
        triggerBanner("ATOMIC DETONATION!");
    }
"""
text = text.replace(
    "private void drawConfetti(Canvas canvas) {",
    trigger_method + "\n    private void drawConfetti(Canvas canvas) {"
)

with open(path, "w") as f:
    f.write(text)

print("Done")
