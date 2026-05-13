package com.mavelastudio.skyfury;

import android.content.Context;
import android.content.SharedPreferences;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Random;

public final class SkyFuryGameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final int SCREEN_MENU = 0;
    private static final int SCREEN_PLAYING = 1;
    private static final int SCREEN_PAUSED = 2;
    private static final int SCREEN_SETTINGS = 3;
    private static final int SCREEN_SHOP = 4;

    private static final int TIER_BASIC = 0;
    private static final int TIER_GENERAL = 1;
    private static final int TIER_BOSS = 2;

    private static final int POWER_GUN = 0;
    private static final int POWER_ROCKET = 1;
    private static final int POWER_ARMOR = 2;
    private static final int POWER_LIFE = 3;
    private static final int POWER_SPECIAL = 4;
    private static final int POWER_BARRAGE = 5;
    private static final int POWER_GUARDIAN = 6;
    private static final int POWER_ATOMIC = 7;
    private static final int POWER_GOLD = 8;
    private static final float PERMANENT_UPGRADE_TIMER = 999999f;

    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final float MAX_DELTA_SECONDS = 0.050f;
    private static final int FPS_GAMEPLAY = 45;
    private static final int FPS_GAMEPLAY_BATTERY = 30;
    private static final int FPS_UI = 25;
    private static final int FPS_PAUSED = 10;

    private static final int MAX_BULLETS = 260;
    private static final int MAX_BULLETS_BATTERY = 190;
    private static final int MAX_ENEMY_SHOTS = 240;
    private static final int MAX_ENEMY_SHOTS_BATTERY = 160;
    private static final int MAX_EXPLOSIONS = 72;
    private static final int MAX_EXPLOSIONS_BATTERY = 42;
    private static final int MAX_SMOKE_PARTICLES = 84;
    private static final int MAX_SMOKE_PARTICLES_BATTERY = 42;
    private static final int MAX_CONFETTI = 120;
    private static final int MAX_CONFETTI_BATTERY = 62;
    private static final int MAX_PICKUP_TEXTS = 8;
    private static final int MAX_CLOUDS = 0;
    private static final int MAX_CLOUDS_BATTERY = 0;
    private static final int MAX_PICKUPS = 28;

    private static final boolean SHOW_DEBUG_COUNTERS = false;
    private static final int[] CLEAR_SKY_COLORS = {
            Color.rgb(105, 184, 234),
            Color.rgb(176, 219, 245),
            Color.rgb(225, 237, 233)
    };
    private static final int[] STORM_SKY_COLORS = {
            Color.rgb(40, 59, 78),
            Color.rgb(80, 91, 105),
            Color.rgb(112, 103, 91)
    };
    private static final float[] SKY_STOPS = {0f, 0.68f, 1f};

    private static final int SLIDER_NONE = 0;
    private static final int SLIDER_MUSIC = 1;
    private static final int SLIDER_SFX = 2;
    private static final int INVALID_POINTER_ID = -1;
    private static final int CONTROL_FIXED = 0;
    private static final int CONTROL_DRAG = 1;

    private static final String PREFS = "sky_fury_settings";
    private static final String PREF_PLANE = "plane";
    private static final String PREF_CONTROL = "control";
    private static final String PREF_HIGH_SCORE = "high_score";
    private static final String PREF_MUSIC = "music";
    private static final String PREF_SFX = "sfx";
    private static final String PREF_BATTERY_SAVER = "battery_saver";
    private static final String PREF_GOLD = "gold";
    private static final String PREF_UNLOCKED = "unlocked_planes";
    private static final String PRODUCT_GOLD_100 = "gold_100";
    private static final String PRODUCT_GOLD_300 = "gold_300";
    private static final String PRODUCT_GOLD_500 = "gold_500";
    private static final String PRODUCT_GOLD_1000 = "gold_1000";

    private static final String[] PLANE_NAMES = {
            "Supermarine Spitfire Mk V",
            "North American P-51D Mustang",
            "Messerschmitt Me 262A",
            "Gloster Meteor F.3",
            "de Havilland Vampire F.1",
            "McDonnell TD2D Katydid",
            "Bell XP-59A Airacomet"
    };
    private static final int[] PLANE_UNLOCK_COSTS = {0, 0, 300, 400, 500, 600, 700};
    private static final boolean[] PLANE_JET = {false, false, true, true, true, true, true};
    // Rebalanced so paid aircraft provide clear value by tier and playstyle.
    // Free planes remain viable, premium planes feel meaningfully stronger or more specialized.
    private static final float[] PLANE_SPEED = {1.05f, 1.00f, 1.28f, 1.14f, 1.26f, 1.33f, 1.24f};
    private static final float[] PLANE_HP = {98f, 106f, 90f, 116f, 96f, 98f, 124f};
    // Damage multiplier per plane (applies to gun, barrage, guardian, and rocket damage).
    private static final float[] PLANE_DAMAGE_MULT = {1.00f, 1.00f, 1.12f, 1.03f, 0.98f, 1.22f, 1.10f};
    // Fire interval multiplier per plane (<1 faster fire, >1 slower fire).
    private static final float[] PLANE_FIRE_INTERVAL_MULT = {1.00f, 1.00f, 1.12f, 0.94f, 0.88f, 1.00f, 0.90f};
    private static final int[] PLAYER_AIRCRAFT_RES = {
            R.drawable.supermarine_spitfire_mk_v,
            R.drawable.north_american_p51d_mustang,
            R.drawable.messerschmitt_me_262a,
            R.drawable.gloster_meteor_f3,
            R.drawable.de_havilland_vampire_f1,
            R.drawable.mcdonnell_td2d_katydid,
            R.drawable.bell_xp59a_airacomet
    };
    private static final int[] POWERUP_RES = {
            R.drawable.gun_upgrade,
            R.drawable.rocket_upgrade,
            R.drawable.armor_upgrade,
            R.drawable.extra_life,
            R.drawable.laser_upgrade,
            R.drawable.gun_spread,
            R.drawable.guardian_angel,
            R.drawable.atomic_bomb
    };
    private static final int[] BOSS_AIRCRAFT_RES = {
            R.drawable.b24_liberator,
            R.drawable.avro_lancaster,
            R.drawable.b29_superfortress,
            R.drawable.b25_mitchell,
            R.drawable.b17_flying_fortress
    };

    private static final int DEFAULT_PLAYER_PRIMARY = Color.rgb(73, 86, 51);
    private static final int DEFAULT_PLAYER_SECONDARY = Color.rgb(111, 82, 52);
    private static final int DEFAULT_PLAYER_MARKING = Color.rgb(43, 77, 145);

    private final SurfaceHolder holder;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint spritePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private final Paint.FontMetrics fontMetrics = new Paint.FontMetrics();
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final RectF spriteRect = new RectF();
    private final Typeface boldTypeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private final Random random = new Random();
    private final ArrayList<Bullet> bullets = new ArrayList<>();
    private final ArrayList<EnemyShot> enemyShots = new ArrayList<>();
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Pickup> pickups = new ArrayList<>();
    private final ArrayList<PickupText> pickupTexts = new ArrayList<>();
    private final ArrayList<Explosion> explosions = new ArrayList<>();
    private final ArrayList<Confetti> confetti = new ArrayList<>();
    private final ArrayList<Cloud> clouds = new ArrayList<>();
    private final SharedPreferences prefs;
    private final GameAudio audio;
    private final PurchaseManager purchaseManager;
    private final Bitmap[] playerAircraftSprites;
    private final Bitmap[] powerupSprites;
    private final Bitmap[] bossAircraftSprites;
    private final Bitmap reviveSprite;
    private final Bitmap goldCoinSprite;
    private final Bitmap gameOverSprite;
    private final Bitmap pausedSprite;
    private final Bitmap shopSprite;
    private final Bitmap coinBagSprite, coinChestSprite, coinChestFullSprite, mountainCoinSprite;
    private final Bitmap restartSprite, mainMenuSprite, startSprite, settingsSprite, resumeSprite;
    private final Bitmap startV2Sprite, settingsV2Sprite, shopV2Sprite;
    private final Bitmap mavelaStudioSprite, scoreLabelSprite, goldObtainedLabelSprite;
    private final Bitmap bestScoreLabelSprite;
    private final Bitmap[] numberSprites;
    private final ColorMatrixColorFilter grayscaleFilter;

    private boolean[] planeUnlocked = new boolean[PLANE_NAMES.length];
    private int gold = 0;
    private int goldEarned = 0;
    private boolean hasRevived = false;
    private final Bitmap basicEnemySprite;
    private final Bitmap generalEnemySprite;
    private final Bitmap welcomeScreenSprite;
    private final Bitmap startBtnSprite;
    private final Bitmap settingsBtnSprite;

    private volatile boolean running;
    private volatile boolean surfaceReady;
    private volatile boolean activityResumed;
    private Thread loopThread;

    private int viewWidth = 1;
    private int viewHeight = 1;
    private int gameScreen = SCREEN_MENU;
    private int settingsReturnScreen = SCREEN_MENU;
    private int activeSlider = SLIDER_NONE;
    private int joystickPointerId = INVALID_POINTER_ID;
    private int specialPointerId = INVALID_POINTER_ID;
    private int atomicPointerId = INVALID_POINTER_ID;
    private int sliderPointerId = INVALID_POINTER_ID;

    // Pop-up System
    private boolean popupActive = false;
    private String popupTitle = "";
    private String popupMessage = "";
    private int popupType = 0; // 0: Confirmation, 1: Info (Aircraft Unlocked), 2: Error (Not enough gold)
    private int targetUnlockPlane = -1;
    private static final int POPUP_CONFIRM = 0;
    private static final int POPUP_SUCCESS = 1;
    private static final int POPUP_ERROR = 2;

    private float totalTime;
    private float playerX;
    private float playerY;
    private float playerHp;
    private float playerTilt;
    private float joystickX;
    private float joystickY;
    private float spawnTimer;
    private float fireTimer;
    private float gunPowerTimer;
    private float barrageTimer;
    private float rocketTimer;
    private float rocketFireTimer;
    private float armorTimer;
    private float guardianTimer;
    private float guardianFireTimer;
    private float laserTimer;
    private float laserDamageTick;
    private float hitFlash;
    private float invulnerableTimer;
    private float damageGraceTimer;
    private float lightningTimer;
    private float lightningCooldown;
    private float lightningX;
    private int lightningSeed;
    private float bannerTimer;
    private float highScoreCelebrationTimer;
    private float musicVolume = 0.68f;
    private float sfxVolume = 0.86f;
    private int score;
    private int highScore;
    private int wave;
    private int waveKills;
    private int lives;
    private int specialCharges;
    private int atomicCharges;
    private int cachedAtomicCharges = -1;
    private String atomicChargeText = "x0";
    private int playerPlane;
    private int controlStyle = CONTROL_FIXED;
    private int primaryColor = DEFAULT_PLAYER_PRIMARY;
    private int secondaryColor = DEFAULT_PLAYER_SECONDARY;
    private int markingColor = DEFAULT_PLAYER_MARKING;
    private int cachedShaderWidth = -1;
    private int cachedShaderHeight = -1;
    private int cachedScore = Integer.MIN_VALUE;
    private int cachedWave = Integer.MIN_VALUE;
    private int cachedLives = Integer.MIN_VALUE;
    private int cachedHp = Integer.MIN_VALUE;
    private int cachedMaxHp = Integer.MIN_VALUE;
    private int cachedSpecialCharges = Integer.MIN_VALUE;
    private int cachedHighScore = Integer.MIN_VALUE;
    private Shader clearSkyShader;
    private Shader stormSkyShader;
    private Shader sunGlowShader;
    private Bitmap clearBackgroundBitmap;
    private Bitmap stormBackgroundBitmap;
    private float dragLastX;
    private float dragLastY;
    private float currentFps;
    private float frameTimeMs;
    private float fpsAccumulator;
    private int fpsFrames;
    private boolean bossActive;
    private boolean gameOver;
    private boolean gameStarted;
    private boolean newHighScore;
    private boolean batterySaver;
    private String bannerText = "";
    private String debugLineOne = "FPS 0  0ms";
    private String debugLineTwo = "B 0  ES 0  X 0  P 0";
    private String scoreText = "SCORE 0";
    private String waveText = "WAVE 1";
    private String livesText = "LIVES 3";
    private String hpText = "HP 0/0";
    private String specialChargeText = "x0";
    private String highScoreText = "HIGH SCORE 0";
    private String musicPercentText = "68%";
    private String sfxPercentText = "86%";

    public SkyFuryGameView(Context context, PurchaseManager purchaseManager) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        audio = new GameAudio(context.getApplicationContext());
        this.purchaseManager = purchaseManager != null ? purchaseManager : new NoOpPurchaseManager();
        playerAircraftSprites = loadBitmaps(PLAYER_AIRCRAFT_RES);
        powerupSprites = loadBitmaps(POWERUP_RES);
        bossAircraftSprites = loadBitmaps(BOSS_AIRCRAFT_RES);
        basicEnemySprite = BitmapFactory.decodeResource(getResources(), R.drawable.grumman_f8f_bearcat);
        generalEnemySprite = BitmapFactory.decodeResource(getResources(), R.drawable.lockheed_p38_lightning);
        welcomeScreenSprite = BitmapFactory.decodeResource(getResources(), R.drawable.welcome_screen);
        startBtnSprite = BitmapFactory.decodeResource(getResources(), R.drawable.start);
        settingsBtnSprite = BitmapFactory.decodeResource(getResources(), R.drawable.settings);
        reviveSprite = BitmapFactory.decodeResource(getResources(), R.drawable.revive);
        goldCoinSprite = BitmapFactory.decodeResource(getResources(), R.drawable.coin);
        gameOverSprite = BitmapFactory.decodeResource(getResources(), R.drawable.game_over);
        pausedSprite = BitmapFactory.decodeResource(getResources(), R.drawable.paused);
        shopSprite = BitmapFactory.decodeResource(getResources(), R.drawable.shop);
        coinBagSprite = BitmapFactory.decodeResource(getResources(), R.drawable.coin_bag);
        coinChestSprite = BitmapFactory.decodeResource(getResources(), R.drawable.coin_chest);
        coinChestFullSprite = BitmapFactory.decodeResource(getResources(), R.drawable.coin_chest_full);
        mountainCoinSprite = BitmapFactory.decodeResource(getResources(), R.drawable.mountain_coin);
        restartSprite = BitmapFactory.decodeResource(getResources(), R.drawable.restart);
        mainMenuSprite = BitmapFactory.decodeResource(getResources(), R.drawable.main_menu);
        startSprite = BitmapFactory.decodeResource(getResources(), R.drawable.start);
        settingsSprite = BitmapFactory.decodeResource(getResources(), R.drawable.settings);
        resumeSprite = BitmapFactory.decodeResource(getResources(), R.drawable.resume);
        startV2Sprite = BitmapFactory.decodeResource(getResources(), R.drawable.start_v2);
        settingsV2Sprite = BitmapFactory.decodeResource(getResources(), R.drawable.settings_v2);
        shopV2Sprite = BitmapFactory.decodeResource(getResources(), R.drawable.shop_v2);
        mavelaStudioSprite = BitmapFactory.decodeResource(getResources(), R.drawable.mavela_studio);
        scoreLabelSprite = BitmapFactory.decodeResource(getResources(), R.drawable.score);
        goldObtainedLabelSprite = BitmapFactory.decodeResource(getResources(), R.drawable.gold_obtained);
        bestScoreLabelSprite = BitmapFactory.decodeResource(getResources(), R.drawable.best_score);
        numberSprites = new Bitmap[] {
                BitmapFactory.decodeResource(getResources(), R.drawable.num_0),
                BitmapFactory.decodeResource(getResources(), R.drawable.num_1),
                BitmapFactory.decodeResource(getResources(), R.drawable.num_2),
                BitmapFactory.decodeResource(getResources(), R.drawable.num_3),
                BitmapFactory.decodeResource(getResources(), R.drawable.num_4),
                BitmapFactory.decodeResource(getResources(), R.drawable.num_5),
                BitmapFactory.decodeResource(getResources(), R.drawable.num_6),
                BitmapFactory.decodeResource(getResources(), R.drawable.num_7),
                BitmapFactory.decodeResource(getResources(), R.drawable.num_8),
                BitmapFactory.decodeResource(getResources(), R.drawable.num_9)
        };

        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0.12f);
        grayscaleFilter = new ColorMatrixColorFilter(cm);

        textPaint.setTypeface(boldTypeface);
        textPaint.setColor(Color.WHITE);
        textPaint.setShadowLayer(4f, 0f, 2f, Color.argb(190, 0, 0, 0));

        loadSettings();
        resetGame();
        gameScreen = SCREEN_MENU;
        gameStarted = false;
    }

    private Bitmap[] loadBitmaps(int[] resourceIds) {
        Bitmap[] bitmaps = new Bitmap[resourceIds.length];
        for (int i = 0; i < resourceIds.length; i++) {
            bitmaps[i] = BitmapFactory.decodeResource(getResources(), resourceIds[i]);
        }
        return bitmaps;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        startLoopIfReady();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        resizeWorld(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        stopLoop();
    }

    @Override
    protected void onDetachedFromWindow() {
        shutdown();
        super.onDetachedFromWindow();
    }

    void onHostResume() {
        activityResumed = true;
        if (gameScreen == SCREEN_PLAYING && !gameOver) {
            audio.pauseMusic();
        } else {
            audio.resumeMusic();
        }
        startLoopIfReady();
    }

    void onHostPause() {
        activityResumed = false;
        resetControls();
        stopLoop();
        audio.pauseMusic();
    }

    void shutdown() {
        activityResumed = false;
        surfaceReady = false;
        resetControls();
        stopLoop();
        recycleBackgroundBitmaps();
        audio.release();
    }

    void resumeAudio() {
        onHostResume();
    }

    void pauseAudio() {
        onHostPause();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            handleTouchDown(event, event.getActionIndex());
        } else if (action == MotionEvent.ACTION_MOVE) {
            handleTouchMove(event);
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            handleTouchUp(event, event.getActionIndex());
        } else if (action == MotionEvent.ACTION_CANCEL) {
            resetControls();
        }
        return true;
    }

    @Override
    public void run() {
        try {
            long lastFrame = System.nanoTime();
            while (running) {
                if (!surfaceReady || !activityResumed) {
                    lastFrame = System.nanoTime();
                    sleepForNanos(NANOS_PER_SECOND / FPS_PAUSED);
                    continue;
                }
                long frameStart = System.nanoTime();
                float dt = clamp((frameStart - lastFrame) / (float) NANOS_PER_SECOND, 0f, MAX_DELTA_SECONDS);
                lastFrame = frameStart;
                updateGame(dt);
                drawFrame();
                long frameEnd = System.nanoTime();
                updatePerformanceCounters(dt, frameEnd - frameStart);
                long remainingNanos = getTargetFrameNanos() - (System.nanoTime() - frameStart);
                sleepForNanos(remainingNanos);
            }
        } finally {
            synchronized (this) {
                if (loopThread == Thread.currentThread()) {
                    loopThread = null;
                }
                running = false;
            }
        }
    }

    private long getTargetFrameNanos() {
        return NANOS_PER_SECOND / Math.max(1, getTargetFps());
    }

    private int getTargetFps() {
        if (gameScreen == SCREEN_PAUSED) {
            return FPS_PAUSED;
        }
        if (gameScreen == SCREEN_PLAYING && !gameOver) {
            return batterySaver ? FPS_GAMEPLAY_BATTERY : FPS_GAMEPLAY;
        }
        return FPS_UI;
    }

    private void updatePerformanceCounters(float dt, long frameNanos) {
        frameTimeMs = frameNanos / 1_000_000f;
        fpsAccumulator += dt;
        fpsFrames++;
        if (fpsAccumulator >= 0.50f) {
            currentFps = fpsFrames / Math.max(0.001f, fpsAccumulator);
            fpsAccumulator = 0f;
            fpsFrames = 0;
            debugLineOne = "FPS " + Math.round(currentFps) + "  " + Math.round(frameTimeMs) + "ms";
            debugLineTwo = "B " + bullets.size()
                    + "  ES " + enemyShots.size()
                    + "  X " + getActiveExplosionCount()
                    + "  P " + getParticleCount();
        }
    }

    private void updateHudTextCache() {
        if (score != cachedScore) {
            cachedScore = score;
            scoreText = "SCORE " + score;
        }
        if (wave != cachedWave) {
            cachedWave = wave;
            waveText = "WAVE " + wave;
        }
        if (lives != cachedLives) {
            cachedLives = lives;
            livesText = "LIVES " + lives;
        }
        int roundedHp = Math.round(Math.max(0f, playerHp));
        int roundedMaxHp = Math.round(getPlayerMaxHp());
        if (roundedHp != cachedHp || roundedMaxHp != cachedMaxHp) {
            cachedHp = roundedHp;
            cachedMaxHp = roundedMaxHp;
            hpText = "HP " + roundedHp + "/" + roundedMaxHp;
        }
        if (atomicCharges != cachedAtomicCharges) {
            cachedAtomicCharges = atomicCharges;
            atomicChargeText = "x" + atomicCharges;
        }
        if (specialCharges != cachedSpecialCharges) {
            cachedSpecialCharges = specialCharges;
            specialChargeText = "x" + specialCharges;
        }
        if (highScore != cachedHighScore) {
            cachedHighScore = highScore;
            highScoreText = "HIGH SCORE " + highScore;
        }
    }

    private void updateVolumeTextCache() {
        musicPercentText = Math.round(musicVolume * 100f) + "%";
        sfxPercentText = Math.round(sfxVolume * 100f) + "%";
    }

    private void startLoopIfReady() {
        synchronized (this) {
            if (!activityResumed || !surfaceReady || running || loopThread != null) {
                return;
            }
            running = true;
            loopThread = new Thread(this, "SkyFuryLoop");
            loopThread.start();
        }
    }

    private void stopLoop() {
        Thread threadToJoin;
        synchronized (this) {
            running = false;
            threadToJoin = loopThread;
        }
        if (threadToJoin != null && threadToJoin != Thread.currentThread()) {
            try {
                threadToJoin.join(900L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        synchronized (this) {
            if (loopThread == threadToJoin) {
                loopThread = null;
            }
        }
    }

    private void handleTouchDown(MotionEvent event, int pointerIndex) {
        int pointerId = event.getPointerId(pointerIndex);
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);
        if (gameScreen == SCREEN_MENU) {
            handleMenuTouch(x, y);
            return;
        }
        if (gameScreen == SCREEN_PAUSED) {
            handlePauseTouch(x, y);
            return;
        }
        if (gameScreen == SCREEN_SETTINGS) {
            handleSettingsTouchDown(pointerId, x, y);
            return;
        }
        if (gameScreen == SCREEN_SHOP) {
            handleShopTouch(x, y);
            return;
        }
        if (gameOver) {
            handleGameOverTouch(x, y);
            return;
        }
        if (isPauseButtonHit(x, y)) {
            audio.playButton();
            pauseGame();
            return;
        }
        if (isSpecialButtonHit(x, y)) {
            specialPointerId = pointerId;
            releaseSpecialWeapon();
        } else if (isAtomicButtonHit(x, y)) {
            atomicPointerId = pointerId;
        } else if (controlStyle == CONTROL_DRAG && joystickPointerId == INVALID_POINTER_ID) {
            joystickPointerId = pointerId;
            dragLastX = x;
            dragLastY = y;
            joystickX = 0f;
            joystickY = 0f;
        } else if (controlStyle == CONTROL_FIXED && x <= viewWidth * 0.55f && joystickPointerId == INVALID_POINTER_ID) {
            joystickPointerId = pointerId;
            updateJoystick(x, y);
        }
    }

    private void handleTouchMove(MotionEvent event) {
        if (gameScreen == SCREEN_SETTINGS) {
            updateActiveSlider(event);
            return;
        }
        if (joystickPointerId != INVALID_POINTER_ID) {
            int pointerIndex = event.findPointerIndex(joystickPointerId);
            if (pointerIndex >= 0) {
                if (controlStyle == CONTROL_DRAG) {
                    updateDragControl(event.getX(pointerIndex), event.getY(pointerIndex));
                } else {
                    updateJoystick(event.getX(pointerIndex), event.getY(pointerIndex));
                }
            }
        }
    }

    private void handleTouchUp(MotionEvent event, int pointerIndex) {
        int pointerId = event.getPointerId(pointerIndex);
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);
        if (pointerId == joystickPointerId) {
            joystickPointerId = INVALID_POINTER_ID;
            joystickX = 0f;
            joystickY = 0f;
        }
        if (pointerId == specialPointerId) {
            specialPointerId = INVALID_POINTER_ID;
        }
        if (pointerId == atomicPointerId) {
            atomicPointerId = INVALID_POINTER_ID;
            if (atomicCharges > 0 && isAtomicButtonHit(x, y)) {
                atomicCharges--;
                triggerAtomicBomb();
            }
        }
        if (pointerId == sliderPointerId) {
            sliderPointerId = INVALID_POINTER_ID;
            activeSlider = SLIDER_NONE;
        }
    }

    private void handleGameOverTouch(float x, float y) {
        if (!hasRevived && isReviveButtonHit(x, y)) {
            audio.playButton();
            revivePlayer();
            return;
        }
        
        float buttonX = viewWidth * 0.5f;
        float bh = getEndMenuButtonHeight();
        float bw = getEndMenuButtonWidth();
        float restartY = getEndRestartButtonCenterY();
        float mainMenuY = getEndMainMenuButtonCenterY();

        if (isInside(x, y, buttonX - bw * 0.5f, restartY - bh * 0.5f, buttonX + bw * 0.5f, restartY + bh * 0.5f)) {
            audio.playButton();
            startNewGame();
        } else if (isInside(x, y, buttonX - bw * 0.5f, mainMenuY - bh * 0.5f, buttonX + bw * 0.5f, mainMenuY + bh * 0.5f)) {
            audio.playButton();
            gameScreen = SCREEN_MENU;
            gameStarted = false;
        }
    }

    private void handleMenuTouch(float x, float y) {
        if (isMenuStartHit(x, y)) {
            audio.playButton();
            startNewGame();
        } else if (isMenuSettingsHit(x, y)) {
            audio.playButton();
            openSettings(SCREEN_MENU);
        } else if (isMenuShopHit(x, y)) {
            audio.playButton();
            gameScreen = SCREEN_SHOP;
        }
    }

    private boolean isMenuShopHit(float x, float y) {
        float cx = getMenuShopCenterX();
        float cy = getMenuButtonTop() + getMenuButtonHeight() * 0.5f;
        float bw = getMenuButtonWidth();
        float bh = getMenuButtonHeight();
        return isInside(x, y, cx - bw * 0.5f, cy - bh * 0.5f, cx + bw * 0.5f, cy + bh * 0.5f);
    }

    private void handlePauseTouch(float x, float y) {
        if (isPauseResumeHit(x, y)) {
            audio.playButton();
            gameScreen = SCREEN_PLAYING;
            audio.pauseMusic();
        } else if (isPauseSettingsHit(x, y)) {
            audio.playButton();
            openSettings(SCREEN_PAUSED);
        } else if (isPauseRestartHit(x, y)) {
            audio.playButton();
            startNewGame();
        } else if (isPauseMenuHit(x, y)) {
            audio.playButton();
            gameScreen = SCREEN_MENU;
            gameStarted = false;
        } else if (isPauseShopHit(x, y)) {
            audio.playButton();
            gameScreen = SCREEN_SHOP;
        }
    }

    private boolean isPauseShopHit(float x, float y) {
        float left = getPauseButtonLeft();
        float top = getPauseButtonTop(4);
        float right = left + getPauseButtonWidth();
        float bottom = top + getPauseButtonHeight();
        return isInside(x, y, left, top, right, bottom);
    }

    private void handleSettingsTouchDown(int pointerId, float x, float y) {
        if (popupActive) {
            handlePopupTouch(x, y);
            return;
        }
        if (isSettingsBackHit(x, y)) {
            audio.playButton();
            if (!planeUnlocked[playerPlane]) {
                playerPlane = 0;
            }
            saveSettings();
            gameScreen = settingsReturnScreen;
            return;
        }
        if (isPrivacyPolicyHit(x, y)) {
            audio.playButton();
            if (getContext() instanceof MainActivity) {
                ((MainActivity) getContext()).openPrivacyPolicy();
            }
            return;
        }
        if (isPlanePrevHit(x, y)) {
            audio.playButton();
            setPlane((playerPlane + PLANE_NAMES.length - 1) % PLANE_NAMES.length);
            return;
        }
        if (isPlaneNextHit(x, y)) {
            audio.playButton();
            setPlane((playerPlane + 1) % PLANE_NAMES.length);
            return;
        }
        // Unlock Banner Hit
        if (!planeUnlocked[playerPlane] && isUnlockBannerHit(x, y)) {
            audio.playButton();
            showUnlockPopup(playerPlane);
        }
        if (isGraphicsFullHit(x, y)) {
            audio.playButton();
            setBatterySaver(false);
            return;
        }
        if (isGraphicsSaverHit(x, y)) {
            audio.playButton();
            setBatterySaver(true);
            return;
        }
        int slider = hitSlider(x, y);
        if (slider != SLIDER_NONE) {
            audio.playButton();
            activeSlider = slider;
            sliderPointerId = pointerId;
            updateSlider(slider, x);
        }
    }

    private void showUnlockPopup(int planeIndex) {
        popupActive = true;
        popupTitle = "CONFIRM UNLOCK";
        popupMessage = "Are you sure you want to unlock this aircraft for " + PLANE_UNLOCK_COSTS[planeIndex] + " gold coins?";
        popupType = POPUP_CONFIRM;
        targetUnlockPlane = planeIndex;
    }

    private void handlePopupTouch(float x, float y) {
        float cx = viewWidth * 0.5f;
        float cy = viewHeight * 0.5f;
        float pw = viewWidth * 0.7f;
        float ph = viewHeight * 0.4f;
        float bw = viewWidth * 0.18f;
        float bh = viewHeight * 0.08f;
        float buttonY = cy + ph * 0.25f;

        if (popupType == POPUP_CONFIRM || popupType == POPUP_ERROR) {
            // Yes/Buy button
            if (isInside(x, y, cx - bw * 1.1f, buttonY, cx - bw * 0.1f, buttonY + bh)) {
                audio.playButton();
                if (popupType == POPUP_CONFIRM) {
                    attemptUnlock(targetUnlockPlane);
                } else {
                    popupActive = false;
                    gameScreen = SCREEN_SHOP;
                }
            }
            // No button
            if (isInside(x, y, cx + bw * 0.1f, buttonY, cx + bw * 1.1f, buttonY + bh)) {
                audio.playButton();
                popupActive = false;
            }
        } else {
            // OK button
            if (isInside(x, y, cx - bw * 0.5f, buttonY, cx + bw * 0.5f, buttonY + bh)) {
                audio.playButton();
                popupActive = false;
            }
        }
    }

    private void attemptUnlock(int planeIndex) {
        int cost = PLANE_UNLOCK_COSTS[planeIndex];
        if (gold >= cost) {
            gold -= cost;
            planeUnlocked[planeIndex] = true;
            saveSettings();
            popupTitle = "SUCCESS";
            popupMessage = "Aircraft unlocked!";
            popupType = POPUP_SUCCESS;
        } else {
            popupTitle = "NOT ENOUGH GOLD";
            popupMessage = "Not enough gold coins. Earn more by playing missions.";
            popupType = POPUP_SUCCESS;
        }
    }

    private boolean isUnlockBannerHit(float x, float y) {
        float cx = viewWidth * 0.50f;
        float cy = getSettingsPreviewCenterY();
        float previewH = getSettingsPreviewHalfHeight();
        return isInside(x, y, cx - previewH, cy - previewH, cx + previewH, cy + previewH);
    }

    private void updateActiveSlider(MotionEvent event) {
        if (sliderPointerId == INVALID_POINTER_ID || activeSlider == SLIDER_NONE) {
            return;
        }
        int pointerIndex = event.findPointerIndex(sliderPointerId);
        if (pointerIndex >= 0) {
            updateSlider(activeSlider, event.getX(pointerIndex));
        }
    }

    private void updateJoystick(float x, float y) {
        float dx = x - getJoystickCenterX();
        float dy = y - getJoystickCenterY();
        float radius = getJoystickRadius();
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance > radius) {
            dx = dx / distance * radius;
            dy = dy / distance * radius;
        }
        joystickX = dx / radius;
        joystickY = dy / radius;
    }

    private void updateDragControl(float x, float y) {
        float size = getPlayerSize();
        float oldX = playerX;
        float oldY = playerY;
        float gain = 1.18f;
        playerX = clamp(playerX + (x - dragLastX) * gain, size * 0.72f, viewWidth - size * 0.72f);
        playerY = clamp(playerY + (y - dragLastY) * gain, viewHeight * 0.34f, viewHeight - size * 0.78f);
        dragLastX = x;
        dragLastY = y;
        playerTilt = lerp(playerTilt, clamp((playerX - oldX) * 0.38f, -16f, 16f), 0.65f);
        if (Math.abs(playerY - oldY) > size * 0.015f) {
            playerTilt = lerp(playerTilt, clamp((playerX - oldX) * 0.38f, -16f, 16f), 0.50f);
        }
    }

    private void resetControls() {
        joystickPointerId = INVALID_POINTER_ID;
        specialPointerId = INVALID_POINTER_ID;
        atomicPointerId = INVALID_POINTER_ID;
        sliderPointerId = INVALID_POINTER_ID;
        activeSlider = SLIDER_NONE;
        joystickX = 0f;
        joystickY = 0f;
        dragLastX = 0f;
        dragLastY = 0f;
    }

    private void loadSettings() {
        playerPlane = Math.max(0, Math.min(PLANE_NAMES.length - 1, prefs.getInt(PREF_PLANE, 0)));
        controlStyle = CONTROL_DRAG;
        highScore = Math.max(0, prefs.getInt(PREF_HIGH_SCORE, 0));
        musicVolume = prefs.getFloat(PREF_MUSIC, audio.getMusicVolume());
        sfxVolume = prefs.getFloat(PREF_SFX, audio.getSoundFxVolume());
        batterySaver = prefs.getBoolean(PREF_BATTERY_SAVER, false);
        gold = prefs.getInt(PREF_GOLD, 0);

        String unlocked = prefs.getString(PREF_UNLOCKED, "0,1");
        if ("2,3".equals(unlocked)) {
            unlocked = "0,1";
        }
        String[] parts = unlocked.split(",");
        for (int i = 0; i < planeUnlocked.length; i++) planeUnlocked[i] = false;
        for (String s : parts) {
            try {
                if (s.length() > 0) {
                    int idx = Integer.parseInt(s.trim());
                    if (idx >= 0 && idx < planeUnlocked.length) planeUnlocked[idx] = true;
                }
            } catch (NumberFormatException ignored) {}
        }
        // Ensure defaults are always unlocked
        planeUnlocked[0] = true;
        planeUnlocked[1] = true;

        updateVolumeTextCache();
        audio.setMusicVolume(musicVolume);
        audio.setSoundFxVolume(sfxVolume);
    }

    private void saveSettings() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < planeUnlocked.length; i++) {
            if (planeUnlocked[i]) {
                if (sb.length() > 0) sb.append(",");
                sb.append(i);
            }
        }
        prefs.edit()
                .putInt(PREF_PLANE, playerPlane)
                .putInt(PREF_CONTROL, controlStyle)
                .putFloat(PREF_MUSIC, musicVolume)
                .putFloat(PREF_SFX, sfxVolume)
                .putBoolean(PREF_BATTERY_SAVER, batterySaver)
                .putInt(PREF_GOLD, gold)
                .putString(PREF_UNLOCKED, sb.toString())
                .apply();
    }

    private void setPlane(int plane) {
        playerPlane = plane;
        playerHp = Math.min(playerHp, getPlayerMaxHp());
        saveSettings();
    }

    private void setControlStyle(int style) {
        controlStyle = style == CONTROL_DRAG ? CONTROL_DRAG : CONTROL_FIXED;
        resetControls();
        saveSettings();
    }

    private void setBatterySaver(boolean enabled) {
        if (batterySaver == enabled) {
            return;
        }
        batterySaver = enabled;
        trimVisualEffectsToCaps();
        seedClouds(false);
        saveSettings();
    }

    private void updateSlider(int slider, float x) {
        float value = clamp((x - getSliderLeft()) / Math.max(1f, getSliderRight() - getSliderLeft()), 0f, 1f);
        if (slider == SLIDER_MUSIC) {
            musicVolume = value;
            audio.setMusicVolume(value);
        } else if (slider == SLIDER_SFX) {
            sfxVolume = value;
            audio.setSoundFxVolume(value);
        }
        updateVolumeTextCache();
        saveSettings();
    }

    private void openSettings(int returnScreen) {
        settingsReturnScreen = returnScreen;
        gameScreen = SCREEN_SETTINGS;
        resetControls();
        audio.resumeMusic();
    }

    private void startNewGame() {
        if (!planeUnlocked[playerPlane]) {
            playerPlane = 0;
            saveSettings();
        }
        resetGame();
        gameStarted = true;
        gameScreen = SCREEN_PLAYING;
        audio.pauseMusic();
    }

    private void pauseGame() {
        if (!gameStarted) {
            return;
        }
        gameScreen = SCREEN_PAUSED;
        resetControls();
        audio.resumeMusic();
    }

    private void resizeWorld(int width, int height) {
        viewWidth = Math.max(1, width);
        viewHeight = Math.max(1, height);
        invalidateBackgroundCache();
        if (playerX <= 1f || playerY <= 1f) {
            playerX = viewWidth * 0.50f;
            playerY = viewHeight * 0.82f;
        }
        seedClouds(true);
        resetControls();
    }

    private void resetGame() {
        bullets.clear();
        enemyShots.clear();
        enemies.clear();
        pickups.clear();
        pickupTexts.clear();
        explosions.clear();
        confetti.clear();
        viewWidth = Math.max(1, getWidth());
        viewHeight = Math.max(1, getHeight());
        playerX = viewWidth * 0.50f;
        playerY = viewHeight * 0.82f;
        playerHp = getPlayerMaxHp();
        playerTilt = 0f;
        joystickX = 0f;
        joystickY = 0f;
        totalTime = 0f;
        spawnTimer = 0.8f;
        fireTimer = 0.2f;
        gunPowerTimer = 0f;
        barrageTimer = 0f;
        rocketTimer = 0f;
        rocketFireTimer = 0f;
        armorTimer = 0f;
        guardianTimer = 0f;
        guardianFireTimer = 0f;
        hasRevived = false;
        goldEarned = 0;
        laserTimer = 0f;
        laserDamageTick = 0f;
        hitFlash = 0f;
        invulnerableTimer = 1.2f;
        damageGraceTimer = 0f;
        lightningTimer = 0f;
        lightningCooldown = 1.6f + random.nextFloat() * 3.0f;
        lightningX = viewWidth * 0.5f;
        lightningSeed = 0;
        bannerTimer = 0f;
        highScoreCelebrationTimer = 0f;
        score = 0;
        wave = 1;
        waveKills = 0;
        lives = 3;
        specialCharges = 0;
        bossActive = false;
        gameOver = false;
        newHighScore = false;
        bannerText = "";
        resetControls();
        seedClouds(false);
    }

    private void seedClouds(boolean keepExisting) {
        if (getMaxClouds() <= 0) {
            clouds.clear();
            return;
        }
        if (keepExisting && !clouds.isEmpty()) {
            return;
        }
        clouds.clear();
        int width = Math.max(viewWidth, 1280);
        int height = Math.max(viewHeight, 720);
        for (int i = 0; i < getMaxClouds(); i++) {
            Cloud cloud = new Cloud();
            cloud.x = random.nextFloat() * width;
            cloud.y = height * (0.05f + random.nextFloat() * 0.62f);
            cloud.size = height * (0.040f + random.nextFloat() * 0.070f);
            cloud.speed = 5f + random.nextFloat() * 15f;
            cloud.direction = random.nextBoolean() ? 1f : -1f;
            cloud.driftTimer = 2.5f + random.nextFloat() * 5.0f;
            cloud.pauseTimer = random.nextFloat() * 4.0f;
            cloud.smoke = random.nextFloat() < 0.08f;
            clouds.add(cloud);
        }
    }

    private void updateGame(float dt) {
        totalTime += dt;
        updateLightning(dt);
        updateExplosions(dt);
        updateConfetti(dt);
        if (gameScreen != SCREEN_PLAYING) {
            bannerTimer = Math.max(0f, bannerTimer - dt);
            return;
        }
        if (!planeUnlocked[playerPlane]) {
            playerPlane = 0;
            saveSettings();
        }
        if (gameOver) {
            return;
        }

        bannerTimer = Math.max(0f, bannerTimer - dt);
        hitFlash = Math.max(0f, hitFlash - dt);
        invulnerableTimer = Math.max(0f, invulnerableTimer - dt);
        damageGraceTimer = Math.max(0f, damageGraceTimer - dt);
        updatePickupTexts(dt);
        armorTimer = Math.max(0f, armorTimer - dt);
        guardianTimer = Math.max(0f, guardianTimer - dt);
        if (laserTimer > 0f) {
            laserTimer = Math.max(0f, laserTimer - dt);
            applyLaserDamage(dt);
        }

        updatePlayer(dt);
        updateSpawning(dt);
        updatePlayerFire(dt);
        updateBullets(dt);
        updateEnemies(dt);
        updateEnemyShots(dt);
        updatePickups(dt);
        resolveBulletHits();
        resolvePlayerHits();
        cleanupEnemies();
    }

    private void updatePlayer(float dt) {
        float size = getPlayerSize();
        float oldX = playerX;
        float speed = Math.max(570f, Math.min(viewWidth, viewHeight) * 1.38f) * PLANE_SPEED[playerPlane];
        playerX = clamp(playerX + joystickX * speed * dt, size * 0.72f, viewWidth - size * 0.72f);
        playerY = clamp(playerY + joystickY * speed * dt, viewHeight * 0.34f, viewHeight - size * 0.78f);
        playerTilt = lerp(playerTilt, clamp((playerX - oldX) * 0.22f, -14f, 14f), Math.min(1f, dt * 12f));
    }

    private void updateSpawning(float dt) {
        if (bossActive) {
            return;
        }
        int threshold = getWaveEnemyTarget();
        if (waveKills >= threshold) {
            spawnBoss();
            return;
        }
        spawnTimer -= dt;
        if (spawnTimer <= 0f) {
            int batch = getEnemySpawnBatch();
            for (int i = 0; i < batch; i++) {
                boolean general = random.nextFloat() < getGeneralEnemyChance();
                spawnEnemy(general);
            }
            spawnTimer = getEnemySpawnDelay();
        }
    }

    private int getWaveEnemyTarget() {
        return (int) Math.round(7f + wave * 2.6f + Math.pow(wave, 1.22));
    }

    private int getEnemySpawnBatch() {
        int batch = 1;
        if (wave >= 4 && random.nextFloat() < Math.min(0.82f, 0.14f + wave * 0.060f)) {
            batch++;
        }
        if (wave >= 7 && random.nextFloat() < Math.min(0.56f, 0.06f + (wave - 6) * 0.055f)) {
            batch++;
        }
        if (wave >= 11 && random.nextFloat() < Math.min(0.35f, (wave - 10) * 0.040f)) {
            batch++;
        }
        return batch;
    }

    private void updatePlayerFire(float dt) {
        fireTimer -= dt;
        if (fireTimer <= 0f) {
            firePlayerGuns();
            fireTimer += getGunInterval();
        }
        if (rocketTimer > 0f) {
            rocketFireTimer -= dt;
            if (rocketFireTimer <= 0f) {
                fireRocket();
                audio.playRocketUpgrade();
                rocketFireTimer = 0.54f;
            }
        }
        if (guardianTimer > 0f) {
            guardianFireTimer -= dt;
            if (guardianFireTimer <= 0f) {
                fireGuardianGuns();
                guardianFireTimer = 0.118f;
            }
        }
    }

    private void updateBullets(float dt) {
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            bullet.x += bullet.vx * dt;
            bullet.y += bullet.vy * dt;
            bullet.life -= dt;
            if (bullet.life <= 0f || bullet.y < -90f || bullet.x < -80f || bullet.x > viewWidth + 80f) {
                bullets.remove(i);
            }
        }
    }

    private void updateEnemies(float dt) {
        for (Enemy enemy : enemies) {
            enemy.hitTimer = Math.max(0f, enemy.hitTimer - dt);
            if (enemy.type == TIER_BOSS) {
                float targetY = viewHeight * 0.19f;
                if (enemy.y < targetY) {
                    enemy.y = lerp(enemy.y, targetY, Math.min(1f, dt * 1.7f));
                }
                enemy.x += enemy.vx * dt;
                enemy.x = clamp(enemy.x, viewWidth * 0.22f, viewWidth * 0.78f);
                updateBossSpecialAttacks(enemy, dt);
                if (!isBossBusy(enemy)) {
                    enemy.fireTimer -= dt;
                }
                if (enemy.fireTimer <= 0f && !isBossBusy(enemy)) {
                    fireBossPattern(enemy);
                }
            } else {
                enemy.y += enemy.vy * dt;
                if (enemy.type == TIER_GENERAL) {
                    float zigzag = (float) Math.sin(totalTime * (4.10f + wave * 0.13f) + enemy.phase) * (150f + wave * 18f);
                    enemy.x += (enemy.vx + zigzag) * dt;
                    if (enemy.x < enemy.size || enemy.x > viewWidth - enemy.size) {
                        enemy.vx = -enemy.vx;
                    }
                    enemy.x = clamp(enemy.x, enemy.size, viewWidth - enemy.size);
                } else {
                    enemy.x += enemy.vx * dt;
                }
                enemy.fireTimer -= dt;
                if (enemy.fireTimer <= 0f && enemy.y > viewHeight * 0.12f && enemy.y < viewHeight * 0.68f) {
                    if (enemy.type == TIER_GENERAL) {
                        fireGeneralEnemyShot(enemy);
                    } else {
                        fireBasicEnemyShot(enemy);
                    }
                    enemy.fireTimer = getEnemyFireDelay(enemy.type);
                }
                if (enemy.y > viewHeight + enemy.size * 1.4f) {
                    enemy.dead = true;
                }
            }
        }
    }

    private void updateEnemyShots(float dt) {
        for (int i = enemyShots.size() - 1; i >= 0; i--) {
            EnemyShot shot = enemyShots.get(i);
            shot.x += shot.vx * dt;
            shot.y += shot.vy * dt;
            shot.life -= dt;
            if (shot.life <= 0f || shot.y > viewHeight + 70f || shot.x < -70f || shot.x > viewWidth + 70f) {
                enemyShots.remove(i);
            }
        }
    }

    private void updatePickups(float dt) {
        for (int i = pickups.size() - 1; i >= 0; i--) {
            Pickup pickup = pickups.get(i);
            pickup.y += pickup.vy * dt;
            pickup.x += (float) Math.sin(totalTime * 3.3f + pickup.phase) * 22f * dt;
            pickup.spin += dt * 170f;
            float radius = getPlayerSize() * 0.72f;
            if (distanceSquared(pickup.x, pickup.y, playerX, playerY) <= square(radius)) {
                applyPickup(pickup.type, pickup.x, pickup.y, pickup.goldAmount);
                pickups.remove(i);
            } else if (pickup.y > viewHeight + 80f) {
                pickups.remove(i);
            }
        }
    }

    private void updatePickupTexts(float dt) {
        for (int i = pickupTexts.size() - 1; i >= 0; i--) {
            PickupText text = pickupTexts.get(i);
            text.age += dt;
            text.y += text.vy * dt;
            text.vy += 520f * dt;
            if (text.age >= text.life || text.y > viewHeight + 80f) {
                pickupTexts.remove(i);
            }
        }
    }

    private void updateExplosions(float dt) {
        for (int i = explosions.size() - 1; i >= 0; i--) {
            Explosion explosion = explosions.get(i);
            explosion.age += dt;
            explosion.x += explosion.vx * dt;
            explosion.y += explosion.vy * dt;
            if (explosion.age >= explosion.life) {
                explosions.remove(i);
            }
        }
    }

    private void updateConfetti(float dt) {
        highScoreCelebrationTimer = Math.max(0f, highScoreCelebrationTimer - dt);
        for (int i = confetti.size() - 1; i >= 0; i--) {
            Confetti piece = confetti.get(i);
            piece.age += dt;
            piece.x += piece.vx * dt;
            piece.y += piece.vy * dt;
            piece.vy += 320f * dt;
            piece.rotation += piece.spin * dt;
            if (piece.age >= piece.life || piece.y > viewHeight + 60f) {
                confetti.remove(i);
            }
        }
    }

    private void updateClouds(float dt) {
        for (Cloud cloud : clouds) {
            if (cloud.pauseTimer > 0f) {
                cloud.pauseTimer -= dt;
            } else {
                float stormPush = bossActive ? 1.65f : 1f;
                cloud.x += cloud.direction * cloud.speed * stormPush * dt;
                cloud.y += (bossActive ? cloud.speed * 0.14f : cloud.speed * 0.04f) * dt;
                cloud.driftTimer -= dt;
                if (cloud.driftTimer <= 0f) {
                    cloud.pauseTimer = 1.2f + random.nextFloat() * 4.2f;
                    cloud.driftTimer = 2.4f + random.nextFloat() * 5.2f;
                    if (random.nextFloat() < 0.35f) {
                        cloud.direction *= -1f;
                    }
                }
            }
            if (cloud.x < -cloud.size * 3.8f || cloud.x > viewWidth + cloud.size * 3.8f || cloud.y > viewHeight + cloud.size * 2.0f) {
                cloud.x = cloud.direction > 0f ? -cloud.size * (1.4f + random.nextFloat() * 2.0f) : viewWidth + cloud.size * (1.4f + random.nextFloat() * 2.0f);
                cloud.y = viewHeight * (0.04f + random.nextFloat() * 0.62f);
                cloud.size = Math.max(viewHeight, 720) * (0.040f + random.nextFloat() * 0.070f);
                cloud.speed = 5f + random.nextFloat() * 15f;
                cloud.pauseTimer = random.nextFloat() * 3.0f;
                cloud.driftTimer = 2.4f + random.nextFloat() * 5.2f;
                cloud.smoke = bossActive ? random.nextFloat() < 0.34f : random.nextFloat() < 0.08f;
            }
        }
    }

    private void updateLightning(float dt) {
        if (!bossActive || gameScreen == SCREEN_MENU || gameScreen == SCREEN_SETTINGS) {
            lightningTimer = 0f;
            lightningCooldown = Math.max(0.8f, lightningCooldown - dt * 0.25f);
            return;
        }
        if (batterySaver) {
            lightningTimer = 0f;
            lightningCooldown = Math.max(3.0f, lightningCooldown - dt * 0.12f);
            return;
        }
        lightningTimer = Math.max(0f, lightningTimer - dt);
        lightningCooldown -= dt;
        if (lightningCooldown <= 0f) {
            lightningTimer = 0.24f;
            lightningCooldown = 2.8f + random.nextFloat() * 5.2f;
            lightningX = viewWidth * (0.14f + random.nextFloat() * 0.72f);
            lightningSeed = random.nextInt(10000);
        }
    }

    private void resolveBulletHits() {
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            boolean consumed = false;
            for (Enemy enemy : enemies) {
                if (enemy.dead) {
                    continue;
                }
                if (isBulletHittingEnemy(bullet, enemy)) {
                    enemy.hp -= bullet.damage;
                    enemy.hitTimer = enemy.type == TIER_BOSS ? 0.24f : 0.16f;
                    addExplosion(bullet.x, bullet.y, bullet.radius * (bullet.rocket ? 3.8f : 1.9f), bullet.rocket ? Color.argb(230, 255, 138, 47) : Color.argb(220, 252, 224, 121));
                    if (bullet.rocket) {
                        splashDamage(bullet.x, bullet.y, bullet.damage * 0.70f, bullet.radius * 8.0f);
                        audio.playExplosion();
                    } else {
                        audio.playHit();
                    }
                    if (enemy.hp <= 0f) {
                        destroyEnemy(enemy);
                    }
                    consumed = true;
                    break;
                }
            }
            if (consumed && i < bullets.size()) {
                bullets.remove(i);
            }
        }
    }

    private boolean isBulletHittingEnemy(Bullet bullet, Enemy enemy) {
        float scale = enemy.type == TIER_BOSS ? enemy.size : enemy.size * (enemy.type == TIER_GENERAL ? 1.05f : 1.0f);
        float wingHalfWidth = scale * (enemy.type == TIER_BOSS ? 1.72f : 1.28f) + bullet.radius;
        float bodyHalfHeight = scale * (enemy.type == TIER_BOSS ? 0.92f : 1.08f) + bullet.radius;
        float dx = bullet.x - enemy.x;
        float dy = bullet.y - enemy.y;
        float ellipse = square(dx / wingHalfWidth) + square(dy / bodyHalfHeight);
        if (ellipse <= 1f) {
            return true;
        }

        float fuselageRadius = getEnemyRadius(enemy) * (enemy.type == TIER_BOSS ? 0.62f : 0.74f) + bullet.radius;
        if (distanceSquared(bullet.x, bullet.y, enemy.x, enemy.y) <= square(fuselageRadius)) {
            return true;
        }

        if (enemy.type == TIER_BOSS) {
            float engineY = enemy.y + enemy.size * 0.34f;
            float engineRadius = enemy.size * 0.18f + bullet.radius;
            return distanceSquared(bullet.x, bullet.y, enemy.x - enemy.size * 0.68f, engineY) <= square(engineRadius)
                    || distanceSquared(bullet.x, bullet.y, enemy.x + enemy.size * 0.68f, engineY) <= square(engineRadius);
        }
        return false;
    }

    private void resolvePlayerHits() {
        if (invulnerableTimer > 0f) {
            return;
        }
        float playerRadius = getPlayerSize() * 0.46f;
        for (int i = enemyShots.size() - 1; i >= 0; i--) {
            EnemyShot shot = enemyShots.get(i);
            if (distanceSquared(shot.x, shot.y, playerX, playerY) <= square(playerRadius + 9f)) {
                enemyShots.remove(i);
                damagePlayer(getEnemyBulletDamage());
                return;
            }
        }
        for (Enemy enemy : enemies) {
            if (!enemy.dead && enemy.type != TIER_BOSS && distanceSquared(enemy.x, enemy.y, playerX, playerY) <= square(getEnemyRadius(enemy) * 0.66f + playerRadius)) {
                enemy.dead = true;
                addExplosion(enemy.x, enemy.y, getEnemyRadius(enemy), Color.argb(230, 255, 128, 53));
                damagePlayer(getEnemyCrashDamage());
                return;
            }
        }
    }

    private void cleanupEnemies() {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            if (enemies.get(i).dead) {
                enemies.remove(i);
            }
        }
    }

    private void spawnEnemy(boolean general) {
        float size = Math.max(44f, Math.min(viewWidth, viewHeight) * (general ? 0.092f : 0.074f));
        Enemy enemy = new Enemy();
        enemy.type = general ? TIER_GENERAL : TIER_BASIC;
        enemy.size = size;
        enemy.x = size + random.nextFloat() * Math.max(1f, viewWidth - size * 2f);
        enemy.y = -size * 1.2f;
        enemy.vx = general
                ? (random.nextBoolean() ? 1f : -1f) * (92f + wave * 16f + getDifficultyCurve() * 9f)
                : (random.nextFloat() - 0.5f) * (30f + wave * 5f + getDifficultyCurve() * 3f);
        enemy.vy = getEnemyFlySpeed(enemy.type) + random.nextFloat() * (wave <= 2 ? 26f : 54f);
        enemy.maxHp = enemy.type == TIER_GENERAL ? getGeneralEnemyHp() : getBasicEnemyHp();
        enemy.hp = enemy.maxHp;
        enemy.fireTimer = getEnemyInitialFireDelay(enemy.type);
        enemy.phase = random.nextFloat() * 6.28f;
        enemies.add(enemy);
    }

    private void spawnBoss() {
        Enemy boss = new Enemy();
        boss.type = TIER_BOSS;
        boss.size = Math.max(118f, Math.min(viewWidth, viewHeight) * 0.22f);
        boss.x = viewWidth * 0.5f;
        boss.y = -boss.size * 1.3f;
        boss.maxHp = getBossHp();
        boss.hp = boss.maxHp;
        boss.fireTimer = 0.9f;
        boss.phase = random.nextFloat() * 6.28f;
        boss.spiralAngle = random.nextFloat() * 6.28f;
        boss.spriteIndex = random.nextInt(Math.max(1, bossAircraftSprites.length));
        enemies.add(boss);
        bossActive = true;
        bannerText = "BOSS FIGHT";
        bannerTimer = 1.35f;
    }

    private float getEarlyEase() {
        if (wave <= 1) {
            return 0f;
        }
        if (wave == 2) {
            return 0.45f;
        }
        if (wave == 3) {
            return 0.75f;
        }
        return 1f;
    }

    private float getEnemySpawnDelay() {
        float ease = getEarlyEase();
        float curve = getDifficultyCurve();
        float delay = 1.55f - ease * 0.42f - curve * 0.075f;
        float variance = Math.max(0.13f, 0.55f - ease * 0.12f - curve * 0.020f);
        return Math.max(0.18f, delay) + random.nextFloat() * variance;
    }

    private float getGeneralEnemyChance() {
        // Slow down the increase of general tier enemies as requested
        return Math.min(0.60f, 0.12f + wave * 0.032f + getDifficultyCurve() * 0.004f);
    }

    private float getEnemyFlySpeed(int tier) {
        float ease = getEarlyEase();
        float base = tier == TIER_GENERAL ? 152f : 98f;
        float curve = getDifficultyCurve();
        return base + ease * 36f + curve * (tier == TIER_GENERAL ? 36f : 27f);
    }

    private float getDifficultyCurve() {
        float base = Math.max(0, wave - 2);
        if (wave >= 8) {
            // Greatly reduce the difficulty factor growth starting from wave 8
            // Base for wave 7 (base 5) is ~8.63. We scale VERY slowly from there.
            return 8.63f + (wave - 7) * 0.22f;
        }
        return (float) Math.pow(base, 1.34);
    }

    private float getBasicEnemyHp() {
        if (wave <= 1) {
            return 10f;
        }
        if (wave == 2) {
            return 13f;
        }
        float curve = getDifficultyCurve();
        return 14f + wave * 5.4f + curve * 3.6f;
    }

    private float getGeneralEnemyHp() {
        if (wave <= 1) {
            return 22f;
        }
        if (wave == 2) {
            return 28f;
        }
        float curve = getDifficultyCurve();
        return 32f + wave * 10.5f + curve * 6.2f;
    }

    private float getEnemyInitialFireDelay(int tier) {
        float ease = getEarlyEase();
        float base = tier == TIER_GENERAL ? 1.30f : 1.82f;
        float curve = getDifficultyCurve();
        return Math.max(tier == TIER_GENERAL ? 0.44f : 0.78f, base - ease * 0.42f - curve * 0.050f)
                + random.nextFloat() * (tier == TIER_GENERAL ? 0.72f : 1.05f);
    }

    private float getEnemyFireDelay(int tier) {
        float ease = getEarlyEase();
        float base = tier == TIER_GENERAL ? 1.18f : 1.88f;
        float floor = tier == TIER_GENERAL ? 0.33f : 0.66f;
        float curve = getDifficultyCurve();
        return Math.max(floor, base - ease * 0.36f - curve * (tier == TIER_GENERAL ? 0.062f : 0.044f))
                + random.nextFloat() * (tier == TIER_GENERAL ? 0.44f : 0.72f);
    }

    private float getBasicShotSpeed() {
        float ease = getEarlyEase();
        return 168f + ease * 32f + getDifficultyCurve() * 18f;
    }

    private float getGeneralShotSpeed() {
        float ease = getEarlyEase();
        return 252f + ease * 48f + getDifficultyCurve() * 27f;
    }

    private float getEnemyAimFactor() {
        return 0.10f + getEarlyEase() * 0.09f + getDifficultyCurve() * 0.012f;
    }

    private float getEnemyPassDamage() {
        return 18f + getEarlyEase() * 7f + getDifficultyCurve() * 3.5f;
    }

    private float getEnemyCrashDamage() {
        return 24f + getEarlyEase() * 9f + getDifficultyCurve() * 4.4f;
    }

    private float getEnemyBulletDamage() {
        return 12f + getEarlyEase() * 6f + getDifficultyCurve() * 2.9f;
    }

    private float getBossHp() {
        // Significantly toughest boss as requested
        return 4800f + wave * 950f + Math.min(12000f, wave * wave * 55f);
    }

    private void firePlayerGuns() {
        float size = getPlayerSize();
        float speed = 745f + (gunPowerTimer > 0f ? 115f : 0f);
        float damage = (6.0f + (gunPowerTimer > 0f ? 4.5f : 0f)) * PLANE_DAMAGE_MULT[playerPlane];
        float noseY = playerY - size * 0.82f;
        addBullet(playerX, noseY, 0f, -speed, damage, false);
        addBullet(playerX - size * 0.32f, playerY - size * 0.20f, -28f, -speed * 0.98f, damage * 0.78f, false);
        addBullet(playerX + size * 0.32f, playerY - size * 0.20f, 28f, -speed * 0.98f, damage * 0.78f, false);
        if (gunPowerTimer > 0f) {
            addBullet(playerX - size * 0.18f, playerY - size * 0.48f, -75f, -speed * 0.92f, damage * 0.70f, false);
            addBullet(playerX + size * 0.18f, playerY - size * 0.48f, 75f, -speed * 0.92f, damage * 0.70f, false);
        }
        if (barrageTimer > 0f) {
            fireBarrageShots(size, speed, damage);
        }
        audio.playShoot();
    }

    private void fireBarrageShots(float size, float speed, float damage) {
        int lanes = gunPowerTimer > 0f ? 9 : 7;
        int half = lanes / 2;
        float spread = gunPowerTimer > 0f ? 310f : 265f;
        for (int i = -half; i <= half; i++) {
            float t = i / (float) Math.max(1, half);
            float vx = t * spread;
            float vy = -speed * (0.82f - Math.abs(t) * 0.13f);
            float muzzleX = playerX + t * size * 0.22f;
            float muzzleY = playerY - size * (0.40f + (1f - Math.abs(t)) * 0.32f);
            addBullet(muzzleX, muzzleY, vx, vy, damage * 0.58f, false);
        }
    }

    private void fireRocket() {
        float size = getPlayerSize();
        float rocketDamage = (28f + wave * 1.5f) * PLANE_DAMAGE_MULT[playerPlane];
        addBullet(playerX - size * 0.38f, playerY - size * 0.12f, -42f, -545f, rocketDamage, true);
        addBullet(playerX + size * 0.38f, playerY - size * 0.12f, 42f, -545f, rocketDamage, true);
        audio.playShoot();
    }

    private void fireGuardianGuns() {
        float size = getPlayerSize();
        float allyOffsetX = size * 1.38f;
        float allyY = playerY + size * 0.20f;
        fireGuardianWing(playerX - allyOffsetX, allyY, -1f);
        fireGuardianWing(playerX + allyOffsetX, allyY, 1f);
        audio.playShoot();
    }

    private void fireGuardianWing(float x, float y, float side) {
        float speed = 705f;
        float mult = PLANE_DAMAGE_MULT[playerPlane];
        addBullet(x, y - getPlayerSize() * 0.46f, side * 18f, -speed, 3.4f * mult, false);
        addBullet(x, y - getPlayerSize() * 0.34f, side * 92f, -speed * 0.93f, 2.8f * mult, false);
        addBullet(x, y - getPlayerSize() * 0.30f, side * 152f, -speed * 0.84f, 2.6f * mult, false);
    }

    private void fireBasicEnemyShot(Enemy enemy) {
        addEnemyShot(enemy.x, enemy.y + getEnemyRadius(enemy) * 0.55f, 0f, getBasicShotSpeed(), 4.7f);
    }

    private void fireGeneralEnemyShot(Enemy enemy) {
        float speed = getGeneralShotSpeed();
        float aim = clamp((playerX - enemy.x) * getEnemyAimFactor(), -68f, 68f);
        float wingOffset = enemy.size * 0.32f;
        addEnemyShot(enemy.x - wingOffset, enemy.y + getEnemyRadius(enemy) * 0.45f, aim - 18f, speed, 4.5f);
        addEnemyShot(enemy.x + wingOffset, enemy.y + getEnemyRadius(enemy) * 0.45f, aim + 18f, speed, 4.5f);
    }

    private void addEnemyShot(float x, float y, float vx, float vy, float life) {
        if (enemyShots.size() >= getMaxEnemyShots()) {
            return;
        }
        EnemyShot shot = new EnemyShot();
        shot.x = x;
        shot.y = y;
        shot.vx = vx;
        shot.vy = vy;
        shot.life = life;
        enemyShots.add(shot);
    }

    private void fireBossPattern(Enemy boss) {
        if (isEnemyShotPressureHigh()) {
            boss.fireTimer = batterySaver ? 0.46f : 0.34f;
            return;
        }
        int availablePatterns = wave >= 4 ? 4 : wave >= 2 ? 3 : 2;
        int pattern = boss.bossPattern % availablePatterns;
        boss.bossPattern++;
        if (pattern == 0) {
            fireBossSpray(boss);
            boss.fireTimer = Math.max(0.58f, 1.18f - wave * 0.055f);
        } else if (pattern == 1) {
            fireBossSpread(boss);
            boss.fireTimer = Math.max(0.64f, 1.28f - wave * 0.052f);
        } else if (pattern == 2) {
            startBossSpiral(boss);
            boss.fireTimer = Math.max(0.64f, 1.26f - wave * 0.050f);
        } else {
            startBossLaser(boss);
            boss.fireTimer = Math.max(1.05f, 1.70f - wave * 0.045f);
        }
    }

    private void fireBossSpread(Enemy boss) {
        float speed = 225f + wave * 13f + Math.max(0, wave - 3) * 10f;
        int lanes = wave >= 7 ? 9 : wave >= 5 ? 7 : 5;
        if (batterySaver || isEnemyShotPressureHigh()) {
            lanes = Math.max(3, lanes - 2);
        }
        int half = lanes / 2;
        float startX = boss.x - boss.size * 0.76f;
        float gap = boss.size * 1.52f / Math.max(1, lanes - 1);
        for (int i = 0; i < lanes; i++) {
            float x = startX + i * gap;
            float offset = i - half;
            addEnemyShot(x, boss.y + getEnemyRadius(boss) * 0.58f, offset * (32f + wave * 2f), speed + Math.abs(offset) * 8f, 4.5f);
        }
    }

    private void fireBossSpray(Enemy boss) {
        float speed = 250f + wave * 15f + Math.max(0, wave - 3) * 8f;
        int lanes = Math.min(11, 5 + (wave / 3) * 2);
        if (batterySaver || isEnemyShotPressureHigh()) {
            lanes = Math.max(3, lanes - 2);
        }
        int half = lanes / 2;
        float aim = clamp((playerX - boss.x) * 0.16f, -90f, 90f);
        for (int i = -half; i <= half; i++) {
            float vx = aim + i * (62f + wave * 2f);
            float vy = speed + Math.abs(i) * 14f;
            addEnemyShot(boss.x + i * boss.size * 0.035f, boss.y + getEnemyRadius(boss) * 0.62f, vx, vy, 4.4f);
        }
        if (wave >= 5) {
            addEnemyShot(boss.x - boss.size * 0.62f, boss.y + boss.size * 0.10f, -120f, speed * 0.92f, 4.2f);
            addEnemyShot(boss.x + boss.size * 0.62f, boss.y + boss.size * 0.10f, 120f, speed * 0.92f, 4.2f);
        }
    }

    private void startBossSpiral(Enemy boss) {
        boss.spiralShotsLeft = Math.min(28, 8 + wave * 3);
        if (batterySaver || isEnemyShotPressureHigh()) {
            boss.spiralShotsLeft = Math.max(6, (int) (boss.spiralShotsLeft * 0.62f));
        }
        boss.spiralShotTimer = 0f;
    }

    private void emitBossSpiralShot(Enemy boss) {
        int arms = wave >= 5 ? 3 : 2;
        if (batterySaver || isEnemyShotPressureHigh()) {
            arms = 2;
        }
        float speed = 215f + wave * 12f + Math.max(0, wave - 3) * 7f;
        float originY = boss.y + getEnemyRadius(boss) * 0.52f;
        for (int i = 0; i < arms; i++) {
            double angle = Math.PI / 2.0 + boss.spiralAngle + i * (Math.PI * 2.0 / arms);
            float vx = (float) Math.cos(angle) * speed;
            float vy = (float) Math.sin(angle) * speed;
            if (vy > 45f) {
                addEnemyShot(boss.x, originY, vx, vy, 4.6f);
            }
        }
        boss.spiralAngle += 0.46f + wave * 0.012f;
        boss.spiralShotsLeft--;
    }

    private void startBossLaser(Enemy boss) {
        boss.laserX = clamp(playerX, getPlayerSize(), viewWidth - getPlayerSize());
        boss.laserWidth = getPlayerSize() * (wave >= 6 ? 1.08f : 0.86f);
        boss.laserWarmup = Math.max(0.72f, 1.02f - wave * 0.025f);
        boss.laserTimer = 0f;
        boss.laserHitTimer = 0f;
    }

    private void updateBossSpecialAttacks(Enemy boss, float dt) {
        if (boss.spiralShotsLeft > 0) {
            boss.spiralShotTimer -= dt;
            if (boss.spiralShotTimer <= 0f) {
                emitBossSpiralShot(boss);
                boss.spiralShotTimer += Math.max(0.062f, 0.112f - wave * 0.004f);
            }
        }
        if (boss.laserWarmup > 0f) {
            boss.laserWarmup -= dt;
            if (boss.laserWarmup <= 0f) {
                boss.laserTimer = Math.min(1.35f, 1.0f + wave * 0.045f);
                boss.laserHitTimer = 0f;
                audio.playLaser();
            }
        }
        if (boss.laserTimer > 0f) {
            boss.laserTimer -= dt;
            boss.laserHitTimer = Math.max(0f, boss.laserHitTimer - dt);
            if (isPlayerInBossLaser(boss) && boss.laserHitTimer <= 0f) {
                damagePlayer(24f + wave * 2.2f);
                boss.laserHitTimer = 0.40f;
            }
        }
    }

    private boolean isBossBusy(Enemy boss) {
        return boss.spiralShotsLeft > 0 || boss.laserWarmup > 0f || boss.laserTimer > 0f;
    }

    private boolean isPlayerInBossLaser(Enemy boss) {
        float halfWidth = boss.laserWidth * 0.5f;
        float beamTop = boss.y + getEnemyRadius(boss) * 0.48f;
        return playerY > beamTop
                && playerX > boss.laserX - halfWidth - getPlayerSize() * 0.25f
                && playerX < boss.laserX + halfWidth + getPlayerSize() * 0.25f;
    }

    private void addBullet(float x, float y, float vx, float vy, float damage, boolean rocket) {
        if (bullets.size() >= getMaxBullets()) {
            bullets.remove(0);
        }
        Bullet bullet = new Bullet();
        bullet.x = x;
        bullet.y = y;
        bullet.vx = vx;
        bullet.vy = vy;
        bullet.damage = damage;
        bullet.rocket = rocket;
        bullet.radius = rocket ? 9.5f : 4.7f;
        bullet.life = rocket ? 3.2f : 2.6f;
        bullets.add(bullet);
    }

    private void splashDamage(float x, float y, float damage, float radius) {
        for (Enemy enemy : enemies) {
            if (!enemy.dead && distanceSquared(x, y, enemy.x, enemy.y) <= square(radius + getEnemyRadius(enemy) * 0.45f)) {
                enemy.hp -= damage;
                enemy.hitTimer = Math.max(enemy.hitTimer, 0.20f);
                if (enemy.hp <= 0f) {
                    destroyEnemy(enemy);
                }
            }
        }
    }

    private void applyLaserDamage(float dt) {
        laserDamageTick += dt;
        float beamWidth = getPlayerSize() * 0.78f;
        float left = playerX - beamWidth * 0.5f;
        float right = playerX + beamWidth * 0.5f;
        float top = -80f;
        float bottom = playerY - getPlayerSize() * 0.55f;
        float dps = (185f + wave * 18f) * 3.0f;
        for (Enemy enemy : enemies) {
            if (enemy.dead) {
                continue;
            }
            float r = getEnemyRadius(enemy) * 0.7f;
            if (enemy.x + r >= left && enemy.x - r <= right && enemy.y + r >= top && enemy.y - r <= bottom) {
                enemy.hp -= dps * dt;
                enemy.hitTimer = 0.20f;
                if (laserDamageTick > 0.055f) {
                    addExplosion(enemy.x + (random.nextFloat() - 0.5f) * r, enemy.y + (random.nextFloat() - 0.5f) * r, r * 0.35f, Color.argb(230, 255, 228, 124));
                }
                if (enemy.hp <= 0f) {
                    destroyEnemy(enemy);
                }
            }
        }
        if (laserDamageTick > 0.055f) {
            laserDamageTick = 0f;
        }
        for (int i = enemyShots.size() - 1; i >= 0; i--) {
            EnemyShot shot = enemyShots.get(i);
            if (shot.x >= left && shot.x <= right && shot.y >= top && shot.y <= bottom) {
                enemyShots.remove(i);
            }
        }
    }

    private void destroyEnemy(Enemy enemy) {
        if (enemy.dead) {
            return;
        }
        enemy.dead = true;
        score += enemy.type == TIER_BOSS ? 1000 + wave * 220 : enemy.type == TIER_GENERAL ? 165 + wave * 12 : 95 + wave * 8;
        if (enemy.type == TIER_BOSS) {
            addBossDefeatEffects(enemy);
            audio.playExplosion();
            audio.playVictorySting();
            bossActive = false;
            wave++;
            waveKills = 0;
            spawnTimer = 1.2f;
            bannerText = "WAVE " + wave;
            bannerTimer = 2.0f;
            dropPickup(enemy.x - getEnemyRadius(enemy) * 0.28f, enemy.y, POWER_SPECIAL);
            dropPickup(enemy.x + getEnemyRadius(enemy) * 0.28f, enemy.y, POWER_LIFE);
        } else {
            addExplosion(enemy.x, enemy.y, getEnemyRadius(enemy) * 1.2f, Color.argb(238, 255, 122, 40));
            addExplosion(enemy.x + getEnemyRadius(enemy) * 0.32f, enemy.y - getEnemyRadius(enemy) * 0.18f, getEnemyRadius(enemy) * 0.62f, Color.argb(220, 246, 215, 91));
            audio.playExplosion();
            waveKills++;
            if (random.nextFloat() < 0.34f) {
                dropPickup(enemy.x, enemy.y, randomPowerType());
            }
        }

        // Random chance for gold (30%)
        if (random.nextFloat() < 0.30f || enemy.type == TIER_BOSS) {
            int amount = enemy.type == TIER_BOSS ? 5 : enemy.type == TIER_GENERAL ? 3 : 1;
            dropPickup(enemy.x, enemy.y, POWER_GOLD, amount);
        }
    }

    private void addBossDefeatEffects(Enemy boss) {
        float radius = getEnemyRadius(boss);
        addExplosion(boss.x, boss.y, radius * 1.9f, Color.argb(245, 255, 118, 36), 0.90f);
        addExplosion(boss.x - radius * 0.55f, boss.y + radius * 0.18f, radius * 1.1f, Color.argb(235, 255, 198, 75), 0.72f);
        addExplosion(boss.x + radius * 0.55f, boss.y + radius * 0.10f, radius * 1.1f, Color.argb(235, 255, 198, 75), 0.72f);
        int burstCount = batterySaver ? 7 : 12;
        for (int i = 0; i < burstCount; i++) {
            float angle = (float) (random.nextFloat() * Math.PI * 2.0);
            float distance = radius * (0.15f + random.nextFloat() * 1.15f);
            float x = boss.x + (float) Math.cos(angle) * distance;
            float y = boss.y + (float) Math.sin(angle) * distance * 0.62f;
            addExplosion(x, y, radius * (0.24f + random.nextFloat() * 0.42f), Color.argb(225, 255, 125, 37), 0.42f + random.nextFloat() * 0.42f);
        }
        int smokeCount = batterySaver ? 12 : 28;
        for (int i = 0; i < smokeCount; i++) {
            float angle = (float) (random.nextFloat() * Math.PI * 2.0);
            float distance = radius * (0.15f + random.nextFloat() * 1.40f);
            float x = boss.x + (float) Math.cos(angle) * distance;
            float y = boss.y + (float) Math.sin(angle) * distance * 0.58f;
            float vx = (float) Math.cos(angle) * (18f + random.nextFloat() * 50f);
            float vy = -28f - random.nextFloat() * 82f;
            int shade = 52 + random.nextInt(48);
            addSmokePuff(x, y, radius * (0.30f + random.nextFloat() * 0.48f), Color.argb(215, shade, shade, shade + 4), vx, vy, 1.55f + random.nextFloat() * 1.35f);
        }
    }

    private int randomPowerType() {
        if (wave >= 8 && enemies.size() >= 5 && random.nextFloat() < 0.08f) {
            return POWER_ATOMIC;
        }
        float value = random.nextFloat();
        if (value < 0.20f) {
            return POWER_GUN;
        }
        if (value < 0.37f) {
            return POWER_ROCKET;
        }
        if (value < 0.53f) {
            return POWER_ARMOR;
        }
        if (value < 0.68f) {
            return POWER_BARRAGE;
        }
        if (value < 0.80f) {
            return POWER_GUARDIAN;
        }
        if (value < 0.92f) {
            return POWER_SPECIAL;
        }
        return POWER_LIFE;
    }

    private void dropPickup(float x, float y, int type) {
        dropPickup(x, y, type, 0);
    }

    private void dropPickup(float x, float y, int type, int goldAmount) {
        if (pickups.size() >= MAX_PICKUPS) {
            pickups.remove(0);
        }
        Pickup pickup = new Pickup();
        pickup.x = x;
        pickup.y = y;
        pickup.vy = 92f;
        pickup.phase = random.nextFloat() * 6.28f;
        pickup.spin = random.nextFloat() * 360f;
        pickup.type = type;
        pickup.goldAmount = goldAmount;
        pickups.add(pickup);
    }

    private void applyPickup(int type, float x, float y, int goldAmount) {
        if (type == POWER_GOLD) {
            gold += goldAmount;
            goldEarned += goldAmount;
            addPickupText("GOLD +" + goldAmount, x, y);
            audio.playPowerup();
            return;
        }
        if (type == POWER_GUN) {
            gunPowerTimer = PERMANENT_UPGRADE_TIMER;
            addPickupText("HEAVY MACHINE GUN", x, y);
            audio.playPowerup();
        } else if (type == POWER_BARRAGE) {
            barrageTimer = PERMANENT_UPGRADE_TIMER;
            addPickupText("GUN BARRAGE", x, y);
            audio.playPowerup();
        } else if (type == POWER_ROCKET) {
            rocketTimer = PERMANENT_UPGRADE_TIMER;
            rocketFireTimer = 0f;
            addPickupText("MISSILE LAUNCHER", x, y);
            audio.playRocketUpgrade();
        } else if (type == POWER_ARMOR) {
            armorTimer = 5.0f;
            addPickupText("IMMORTALITY", x, y);
            audio.playArmorUpgrade();
        } else if (type == POWER_LIFE) {
            lives = Math.min(5, lives + 1);
            addPickupText("EXTRA LIFE", x, y);
            audio.playExtraLife();
        } else if (type == POWER_GUARDIAN) {
            guardianTimer = 5.0f;
            guardianFireTimer = 0f;
            addPickupText("GUARDIAN ANGEL", x, y);
            audio.playGuardianAngel();
        } else if (type == POWER_ATOMIC) {
            atomicCharges = Math.min(3, atomicCharges + 1);
            addPickupText("ATOMIC BOMB", x, y);
            audio.playPowerup();
        } else {
            specialCharges = Math.min(3, specialCharges + 1);
            addPickupText("LASER STORED", x, y);
            audio.playPowerup();
        }
    }

    private void addPickupText(String text, float x, float y) {
        if (pickupTexts.size() >= MAX_PICKUP_TEXTS) {
            pickupTexts.remove(0);
        }
        PickupText pickupText = new PickupText();
        pickupText.text = text;
        pickupText.x = clamp(x, viewWidth * 0.18f, viewWidth * 0.82f);
        pickupText.y = y;
        pickupText.vy = 210f;
        pickupText.life = 1.35f;
        pickupTexts.add(pickupText);
    }

    private void releaseSpecialWeapon() {
        if (laserTimer > 0f || gameOver) {
            return;
        }
        if (specialCharges <= 0) {
            bannerText = "LASER EMPTY";
            bannerTimer = 0.9f;
            return;
        }
        specialCharges--;
        laserTimer = 1.5f;
        laserDamageTick = 0f;
        bannerText = "HUGE LASER";
        bannerTimer = 1.2f;
        audio.playLaserUpgrade();
    }

    private void damagePlayer(float damage) {
        if (gameOver || invulnerableTimer > 0f || damageGraceTimer > 0f) {
            return;
        }
        float actualDamage = armorTimer > 0f ? 0f : damage;
        playerHp -= actualDamage;
        hitFlash = 0.48f;
        damageGraceTimer = 0.36f;
        audio.playHit();
        if (armorTimer > 0f) {
            addPickupText("IMMORTALITY", playerX, playerY);
            return;
        }
        if (playerHp > 0f) {
            bannerText = "HULL HIT";
            bannerTimer = 0.85f;
            return;
        }

        lives--;
        audio.playLifeLost();
        resetWeaponUpgradesOnDeath();
        addExplosion(playerX, playerY, getPlayerSize() * 0.92f, Color.argb(238, 255, 80, 50));
        audio.playExplosion();
        if (lives <= 0) {
            playerHp = 0f;
            finishMissionFailed();
        } else {
            playerHp = getPlayerMaxHp();
            playerX = viewWidth * 0.50f;
            playerY = viewHeight * 0.82f;
            invulnerableTimer = 1.8f;
            bannerText = "LIFE LOST";
            bannerTimer = 1.3f;
        }
    }

    private void finishMissionFailed() {
        gameOver = true;
        bannerText = "";
        bannerTimer = 0f;
        if (score > highScore) {
            highScore = score;
            newHighScore = true;
            highScoreCelebrationTimer = 4.2f;
            prefs.edit().putInt(PREF_HIGH_SCORE, highScore).apply();
            spawnHighScoreConfetti();
            audio.playHighScoreFanfare();
        } else {
            newHighScore = false;
            highScoreCelebrationTimer = 0f;
        }

        // Show interstitial ad on game over
        if (getContext() instanceof MainActivity) {
            ((MainActivity) getContext()).showInterstitialAd();
        }
    }

    private void resetWeaponUpgradesOnDeath() {
        gunPowerTimer = 0f;
        barrageTimer = 0f;
        rocketTimer = 0f;
        rocketFireTimer = 0f;
        guardianTimer = 0f;
        guardianFireTimer = 0f;
    }

    private void drawFrame() {
        if (!surfaceReady) {
            return;
        }
        Canvas canvas;
        try {
            canvas = lockGameCanvas();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            surfaceReady = false;
            return;
        }
        if (canvas == null) {
            return;
        }
        try {
            drawGame(canvas);
        } catch (RuntimeException exception) {
            running = false;
        } finally {
            try {
                holder.unlockCanvasAndPost(canvas);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                surfaceReady = false;
            }
        }
    }

    private Canvas lockGameCanvas() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                return holder.lockHardwareCanvas();
            } catch (IllegalArgumentException | IllegalStateException ignored) {
                return holder.lockCanvas();
            }
        }
        return holder.lockCanvas();
    }

    private void drawGame(Canvas canvas) {
        viewWidth = canvas.getWidth();
        viewHeight = canvas.getHeight();
        drawBackground(canvas);
        if (gameScreen == SCREEN_MENU) {
            drawWelcome(canvas);
        } else if (gameScreen == SCREEN_SETTINGS) {
            drawSettings(canvas);
        } else if (gameScreen == SCREEN_SHOP) {
            drawShop(canvas);
        } else {
            drawPickups(canvas);
            drawEnemyShots(canvas);
            drawBullets(canvas);
            drawEnemies(canvas);
            drawBossWeapons(canvas);
            if (laserTimer > 0f) {
                drawLaser(canvas);
            }
            drawPlayer(canvas);
            drawExplosions(canvas);
            drawHud(canvas);
            drawPickupTexts(canvas);
            if (gameScreen == SCREEN_PLAYING && !gameOver) {
                drawControls(canvas);
            }
            if (gameScreen == SCREEN_PAUSED) {
                drawPaused(canvas);
            }
            if (gameOver && gameScreen == SCREEN_PLAYING) {
                drawGameOver(canvas);
            }
        }
        if (bannerTimer > 0f && bannerText.length() > 0) {
            drawBanner(canvas);
        }
    }

    private void drawBackground(Canvas canvas) {
        boolean storm = bossActive && gameScreen != SCREEN_MENU && gameScreen != SCREEN_SETTINGS;
        Bitmap background = getBackgroundBitmap(storm);
        if (background != null) {
            canvas.drawBitmap(background, 0f, 0f, null);
        } else {
            drawBackgroundLayers(canvas, storm);
        }
        if (storm) {
            drawLightning(canvas);
        }
    }

    private void drawBackgroundLayers(Canvas canvas, boolean storm) {
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStrokeWidth(1f);
        paint.setAlpha(255);
        canvas.drawColor(storm ? Color.rgb(40, 59, 78) : Color.rgb(105, 184, 234));
        ensureSkyShaders();
        paint.setShader(storm ? stormSkyShader : clearSkyShader);
        canvas.drawRect(0f, 0f, viewWidth, viewHeight, paint);
        paint.setShader(null);

        if (storm) {
            paint.setColor(Color.argb(68, 20, 26, 34));
            canvas.drawRect(0f, 0f, viewWidth, viewHeight, paint);
        } else {
            paint.setShader(sunGlowShader);
            canvas.drawCircle(viewWidth * 0.82f, viewHeight * 0.18f, viewHeight * 0.30f, paint);
            paint.setShader(null);
        }

        paint.setColor(storm ? Color.argb(82, 42, 42, 43) : Color.argb(34, 76, 112, 120));
        path.reset();
        path.moveTo(0f, viewHeight * 0.82f);
        path.cubicTo(viewWidth * 0.22f, viewHeight * 0.70f, viewWidth * 0.50f, viewHeight * 0.88f, viewWidth, viewHeight * 0.73f);
        path.lineTo(viewWidth, viewHeight);
        path.lineTo(0f, viewHeight);
        path.close();
        canvas.drawPath(path, paint);

        paint.setStrokeWidth(2f);
        paint.setColor(storm ? Color.argb(42, 132, 137, 142) : Color.argb(54, 128, 188, 218));
        for (int i = 0; i < 8; i++) {
            float y = viewHeight * (0.76f + i * 0.033f);
            canvas.drawLine(0f, y, viewWidth, y, paint);
        }
    }

    private Bitmap getBackgroundBitmap(boolean storm) {
        Bitmap cached = storm ? stormBackgroundBitmap : clearBackgroundBitmap;
        if (cached != null && !cached.isRecycled() && cached.getWidth() == viewWidth && cached.getHeight() == viewHeight) {
            return cached;
        }

        Bitmap rendered = createBackgroundBitmap(storm);
        if (rendered == null) {
            return null;
        }
        if (storm) {
            stormBackgroundBitmap = rendered;
        } else {
            clearBackgroundBitmap = rendered;
        }
        return rendered;
    }

    private Bitmap createBackgroundBitmap(boolean storm) {
        if (viewWidth <= 1 || viewHeight <= 1) {
            return null;
        }
        try {
            Bitmap bitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.RGB_565);
            Canvas bitmapCanvas = new Canvas(bitmap);
            drawBackgroundLayers(bitmapCanvas, storm);
            return bitmap;
        } catch (OutOfMemoryError ignored) {
            return null;
        }
    }

    private void invalidateBackgroundCache() {
        clearBackgroundBitmap = null;
        stormBackgroundBitmap = null;
        cachedShaderWidth = -1;
        cachedShaderHeight = -1;
    }

    private void recycleBackgroundBitmaps() {
        if (clearBackgroundBitmap != null) {
            clearBackgroundBitmap.recycle();
            clearBackgroundBitmap = null;
        }
        if (stormBackgroundBitmap != null) {
            stormBackgroundBitmap.recycle();
            stormBackgroundBitmap = null;
        }
    }

    private void ensureSkyShaders() {
        if (cachedShaderWidth == viewWidth && cachedShaderHeight == viewHeight) {
            return;
        }
        cachedShaderWidth = viewWidth;
        cachedShaderHeight = viewHeight;
        clearSkyShader = new LinearGradient(0f, 0f, 0f, viewHeight, CLEAR_SKY_COLORS, SKY_STOPS, Shader.TileMode.CLAMP);
        stormSkyShader = new LinearGradient(0f, 0f, 0f, viewHeight, STORM_SKY_COLORS, SKY_STOPS, Shader.TileMode.CLAMP);
        sunGlowShader = new RadialGradient(viewWidth * 0.82f, viewHeight * 0.18f, viewHeight * 0.30f,
                Color.argb(120, 255, 242, 174), Color.TRANSPARENT, Shader.TileMode.CLAMP);
    }

    private void drawCloud(Canvas canvas, Cloud cloud, boolean storm) {
        int alpha = storm ? (cloud.smoke ? 82 : 118) : (cloud.smoke ? 42 : 96);
        paint.setColor(storm
                ? (cloud.smoke ? Color.argb(alpha, 57, 59, 65) : Color.argb(alpha, 139, 151, 162))
                : (cloud.smoke ? Color.argb(alpha, 150, 154, 154) : Color.argb(alpha, 250, 253, 250)));
        float s = cloud.size;
        float x = cloud.x;
        float y = cloud.y;
        canvas.drawOval(x - s * 1.6f, y - s * 0.35f, x + s * 1.6f, y + s * 0.45f, paint);
        canvas.drawOval(x - s * 0.95f, y - s * 0.75f, x + s * 0.10f, y + s * 0.36f, paint);
        canvas.drawOval(x - s * 0.08f, y - s * 0.82f, x + s * 1.12f, y + s * 0.38f, paint);
    }

    private void drawLightning(Canvas canvas) {
        if (lightningTimer <= 0f) {
            return;
        }
        float flash = clamp(lightningTimer / 0.24f, 0f, 1f);
        paint.setColor(Color.argb((int) (88f * flash), 220, 232, 255));
        canvas.drawRect(0f, 0f, viewWidth, viewHeight, paint);

        float x = lightningX;
        float y = -10f;
        int seed = lightningSeed;
        path.reset();
        path.moveTo(x, y);
        int segments = 7;
        for (int i = 1; i <= segments; i++) {
            seed = seed * 1103515245 + 12345;
            float jitter = (((seed >>> 16) & 255) / 255f - 0.5f) * viewWidth * 0.10f;
            x = clamp(lightningX + jitter, viewWidth * 0.06f, viewWidth * 0.94f);
            y = viewHeight * (0.02f + i * 0.095f);
            path.lineTo(x, y);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Math.max(5f, viewHeight * 0.010f));
        paint.setColor(Color.argb((int) (210f * flash), 178, 206, 255));
        canvas.drawPath(path, paint);
        paint.setStrokeWidth(Math.max(2f, viewHeight * 0.004f));
        paint.setColor(Color.argb((int) (255f * flash), 255, 255, 245));
        canvas.drawPath(path, paint);

        float branchStartY = viewHeight * 0.28f;
        for (int branch = -1; branch <= 1; branch += 2) {
            path.reset();
            path.moveTo(lightningX + branch * viewWidth * 0.02f, branchStartY);
            path.lineTo(lightningX + branch * viewWidth * 0.13f, branchStartY + viewHeight * 0.10f);
            path.lineTo(lightningX + branch * viewWidth * 0.19f, branchStartY + viewHeight * 0.18f);
            paint.setStrokeWidth(Math.max(1.5f, viewHeight * 0.003f));
            canvas.drawPath(path, paint);
        }
        paint.setStrokeCap(Paint.Cap.BUTT);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawWelcome(Canvas canvas) {
        spriteRect.set(0, 0, viewWidth, viewHeight);
        canvas.drawBitmap(welcomeScreenSprite, null, spriteRect, spritePaint);

        if (mavelaStudioSprite != null) {
            drawBitmapCentered(canvas, mavelaStudioSprite, viewWidth * 0.50f, viewHeight * 0.962f, viewHeight * 0.078f, 0f, spritePaint);
        } else {
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(viewHeight * 0.032f);
            textPaint.setColor(Color.argb(205, 255, 255, 255));
            drawIronblockText(canvas, "MAVELA STUDIO", viewWidth * 0.50f, viewHeight * 0.958f, textPaint);
        }

        float menuButtonCy = getMenuButtonTop() + getMenuButtonHeight() * 0.5f;
        float menuButtonH = getMenuButtonHeight();
        float menuButtonW = getMenuButtonWidth();
        float commonMenuH = menuButtonH;
        if (startV2Sprite != null) {
            commonMenuH = Math.min(commonMenuH, menuButtonW * 0.92f * ((float) startV2Sprite.getHeight() / (float) startV2Sprite.getWidth()));
        }
        if (settingsV2Sprite != null) {
            commonMenuH = Math.min(commonMenuH, menuButtonW * 0.92f * ((float) settingsV2Sprite.getHeight() / (float) settingsV2Sprite.getWidth()));
        }
        if (shopV2Sprite != null) {
            commonMenuH = Math.min(commonMenuH, menuButtonW * 0.92f * ((float) shopV2Sprite.getHeight() / (float) shopV2Sprite.getWidth()));
        }
        // v2 text sprites have generous transparent padding; scale up uniformly.
        commonMenuH *= 1.55f;
        if (startV2Sprite != null) {
            drawBitmapCentered(canvas, startV2Sprite, getMenuStartLeft() + menuButtonW * 0.5f, menuButtonCy, commonMenuH, 0f, spritePaint);
        } else {
            drawButton(canvas, "START", getMenuStartLeft(), getMenuButtonTop(), getMenuStartRight(), getMenuButtonBottom(), true, false);
        }

        if (settingsV2Sprite != null) {
            drawBitmapCentered(canvas, settingsV2Sprite, getMenuSettingsLeft() + menuButtonW * 0.5f, menuButtonCy, commonMenuH, 0f, spritePaint);
        } else {
            drawButton(canvas, "SETTINGS", getMenuSettingsLeft(), getMenuButtonTop(), getMenuSettingsRight(), getMenuButtonBottom(), true, false);
        }

        // Shop button to the right of settings
        float shopX = getMenuShopCenterX();
        float shopY = menuButtonCy;
        float sw = menuButtonW;
        float sh = menuButtonH;
        if (shopV2Sprite != null) {
            drawBitmapCentered(canvas, shopV2Sprite, shopX, shopY, commonMenuH, 0f, spritePaint);
        } else {
            drawButton(canvas, "SHOP", shopX - sw * 0.5f, shopY - sh * 0.5f, shopX + sw * 0.5f, shopY + sh * 0.5f, true, false);
        }
    }

    private void drawPaused(Canvas canvas) {
        paint.setColor(Color.argb(160, 3, 7, 12));
        canvas.drawRect(0f, 0f, viewWidth, viewHeight, paint);
        
        // Paused Banner
        float bannerW = Math.min(viewWidth * 0.78f, 680f);
        float bannerH = bannerW * 0.28f;
        spriteRect.set(viewWidth * 0.50f - bannerW * 0.50f, viewHeight * 0.24f - bannerH * 0.50f, viewWidth * 0.50f + bannerW * 0.50f, viewHeight * 0.24f + bannerH * 0.50f);
        canvas.drawBitmap(pausedSprite, null, spriteRect, spritePaint);

        float pauseButtonScale = getPauseButtonHeight() * 1.10f;
        if (resumeSprite != null) {
            drawBitmapCentered(canvas, resumeSprite, getPauseButtonLeft() + getPauseButtonWidth() * 0.5f, getPauseButtonTop(0) + getPauseButtonHeight() * 0.5f, pauseButtonScale, 0f, spritePaint);
        } else {
            drawButton(canvas, "RESUME", getPauseButtonLeft(), getPauseButtonTop(0), getPauseButtonLeft() + getPauseButtonWidth(), getPauseButtonTop(0) + getPauseButtonHeight(), true, true);
        }
        
        if (settingsSprite != null) {
            drawBitmapCentered(canvas, settingsSprite, getPauseButtonLeft() + getPauseButtonWidth() * 0.5f, getPauseButtonTop(1) + getPauseButtonHeight() * 0.5f, pauseButtonScale, 0f, spritePaint);
        } else {
            drawButton(canvas, "SETTINGS", getPauseButtonLeft(), getPauseButtonTop(1), getPauseButtonLeft() + getPauseButtonWidth(), getPauseButtonTop(1) + getPauseButtonHeight(), true, false);
        }

        if (restartSprite != null) {
            drawBitmapCentered(canvas, restartSprite, getPauseButtonLeft() + getPauseButtonWidth() * 0.5f, getPauseButtonTop(2) + getPauseButtonHeight() * 0.5f, pauseButtonScale, 0f, spritePaint);
        } else {
            drawButton(canvas, "RESTART", getPauseButtonLeft(), getPauseButtonTop(2), getPauseButtonLeft() + getPauseButtonWidth(), getPauseButtonTop(2) + getPauseButtonHeight(), true, false);
        }
        if (mainMenuSprite != null) {
            drawBitmapCentered(canvas, mainMenuSprite, getPauseButtonLeft() + getPauseButtonWidth() * 0.5f, getPauseButtonTop(3) + getPauseButtonHeight() * 0.5f, pauseButtonScale, 0f, spritePaint);
        } else {
            drawButton(canvas, "MAIN MENU", getPauseButtonLeft(), getPauseButtonTop(3), getPauseButtonLeft() + getPauseButtonWidth(), getPauseButtonTop(3) + getPauseButtonHeight(), true, false);
        }
        if (shopSprite != null) {
            drawBitmapCentered(canvas, shopSprite, getPauseButtonLeft() + getPauseButtonWidth() * 0.5f, getPauseButtonTop(4) + getPauseButtonHeight() * 0.5f, pauseButtonScale, 0f, spritePaint);
        } else {
            drawButton(canvas, "SHOP", getPauseButtonLeft(), getPauseButtonTop(4), getPauseButtonLeft() + getPauseButtonWidth(), getPauseButtonTop(4) + getPauseButtonHeight(), true, false);
        }
    }

    private void drawSettingsPanel(Canvas canvas) {
        paint.setColor(Color.argb(145, 4, 8, 13));
        canvas.drawRect(0f, 0f, viewWidth, viewHeight, paint);
        float left = getSettingsPanelLeft();
        float top = getSettingsPanelTop();
        float right = getSettingsPanelRight();
        float bottom = getSettingsPanelBottom();
        rect.set(left, top, right, bottom);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(222, 22, 33, 43));
        canvas.drawRoundRect(rect, 8f, 8f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        paint.setColor(Color.argb(195, 231, 218, 178));
        canvas.drawRoundRect(rect, 8f, 8f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawSettingsBack(Canvas canvas) {
        drawButton(canvas, "BACK", getSettingsBackLeft(), getSettingsBackTop(), getSettingsBackRight(), getSettingsBackBottom(), true, false);
    }

    private void drawSettings(Canvas canvas) {
        drawSettingsPanel(canvas);
        float right = getSettingsPanelRight();
        float top = getSettingsPanelTop();
        float left = getSettingsPanelLeft();

        // Gold Owned (Top Right)
        float goldX = right - viewWidth * 0.090f;
        float goldY = top + viewHeight * 0.044f;
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTextSize(Math.max(16f, viewHeight * 0.029f));
        textPaint.setColor(Color.WHITE);
        float coinSize = Math.max(16f, viewHeight * 0.030f);
        float ownedDigitH = coinSize * 0.90f;
        float ownedDigitGap = Math.max(2f, coinSize * 0.07f);
        drawSpriteNumberRightAligned(canvas, String.valueOf(gold), goldX, goldY - coinSize * 0.02f, ownedDigitH, ownedDigitGap);
        float ownedValueW = measureSpriteNumberWidth(String.valueOf(gold), ownedDigitH, ownedDigitGap);
        drawBitmapCentered(canvas, goldCoinSprite, goldX - ownedValueW - coinSize * 0.95f, goldY - coinSize * 0.08f, coinSize, 0f, spritePaint);

        drawSettingsBack(canvas);
        if (settingsSprite != null) {
            drawBitmapCentered(canvas, settingsSprite, viewWidth * 0.50f, top + (isPortrait() ? viewHeight * 0.090f : viewHeight * 0.090f), viewHeight * 0.046f, 0f, spritePaint);
        } else {
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(boldTypeface);
            textPaint.setTextSize(fitDisplayTextSize("SETTINGS", right - left - (isPortrait() ? viewWidth * 0.22f : 0f),
                    isPortrait() ? clamp(viewWidth * 0.11f, 36f, 56f) : Math.max(24f, viewHeight * 0.055f), 24f));
            textPaint.setColor(Color.rgb(255, 229, 176));
            drawIronblockText(canvas, "SETTINGS", viewWidth * 0.50f, top + (isPortrait() ? viewHeight * 0.090f : viewHeight * 0.090f), textPaint);
        }

        float previewX = getSettingsPreviewCenterX();
        float previewY = getSettingsPreviewCenterY();
        float previewHalfWidth = getSettingsPreviewHalfWidth();
        float previewHalfHeight = getSettingsPreviewHalfHeight();
        rect.set(previewX - previewHalfWidth, previewY - previewHalfHeight, previewX + previewHalfWidth, previewY + previewHalfHeight);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(76, 255, 255, 255));
        canvas.drawRoundRect(rect, 8f, 8f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(Color.argb(100, 245, 238, 205));
        canvas.drawRoundRect(rect, 8f, 8f, paint);
        paint.setStyle(Paint.Style.FILL);
        drawAircraft(canvas, previewX, previewY, Math.min(viewWidth, viewHeight) * (isPortrait() ? 0.145f : 0.17f), 0f, true, playerPlane);
        
        // Aircraft Name
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(Math.max(18f, viewHeight * 0.038f));
        textPaint.setColor(Color.WHITE);
        float nameWidth = Math.max(viewWidth * 0.24f, getSelectorNameRight() - getSelectorNameLeft());
        float mustangSize = fitDisplayTextSize("North American P-51D Mustang", nameWidth, textPaint.getTextSize(), 14f);
        textPaint.setTextSize(mustangSize);
        drawIronblockText(canvas, PLANE_NAMES[playerPlane], (getSelectorNameLeft() + getSelectorNameRight()) * 0.5f, getSelectorTop() + getSmallButtonHeight() * 0.88f, textPaint);

        // Unlock Banner
        if (!planeUnlocked[playerPlane]) {
            float bannerW = previewHalfWidth * 1.90f;
            float bannerH = bannerW * 0.16f;
            float bannerY = (previewY + previewHalfHeight) - bannerH * 0.70f;
            rect.set(previewX - bannerW * 0.5f, bannerY - bannerH * 0.5f, previewX + bannerW * 0.5f, bannerY + bannerH * 0.5f);
            paint.setColor(Color.argb(200, 0, 0, 0));
            canvas.drawRoundRect(rect, 10f, 10f, paint);
            
            textPaint.setTextSize(Math.max(11f, viewHeight * 0.022f));
            String unlockText = "UNLOCK FOR ";
            float textW = textPaint.measureText(unlockText);
            float iconS = bannerH * 0.65f;
            String priceStr = String.valueOf(PLANE_UNLOCK_COSTS[playerPlane]);
            float priceDigitH = iconS * 0.84f;
            float priceGap = Math.max(2f, iconS * 0.05f);
            float priceW = measureSpriteNumberWidth(priceStr, priceDigitH, priceGap);
            float totalW = textW + priceW + iconS * 1.25f;
            
            drawIronblockText(canvas, unlockText, previewX - totalW * 0.5f + textW * 0.5f, bannerY + bannerH * 0.15f, textPaint);
            float priceLeft = previewX + totalW * 0.5f - priceW;
            drawBitmapCentered(canvas, goldCoinSprite, priceLeft - iconS * 0.62f, bannerY, iconS, 0f, spritePaint);
            drawSpriteNumberCentered(canvas, priceStr, priceLeft + priceW * 0.5f, bannerY, priceDigitH, priceGap);
        }

        // Show Stats as Bar Chart
        float statsY = previewY + previewHalfHeight + (isPortrait() ? viewHeight * 0.045f : viewHeight * 0.042f);
        float barW = previewHalfWidth * 1.6f;
        float barH = isPortrait() ? 16f : 20f;
        float barX = previewX - barW * 0.5f;
        
        float statLabelSize = getSettingsLabelTextSize() * 0.95f;
        textPaint.setTextSize(statLabelSize);
        textPaint.setTextAlign(Paint.Align.LEFT);
        
        // Speed Bar
        textPaint.setColor(Color.rgb(200, 210, 200));
        textPaint.setTextSize(statLabelSize);
        drawIronblockText(canvas, "SPEED", barX, statsY, textPaint);
        float speedVal = PLANE_SPEED[playerPlane] * 10f;
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTextSize(Math.max(11f, viewHeight * 0.019f));
        drawIronblockText(canvas, String.valueOf(Math.round(speedVal)), barX + barW, statsY - barH * 0.20f, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
        drawStatBar(canvas, barX, statsY + 8f, barW, barH, speedVal / 25f, Color.rgb(100, 180, 255));
        
        // Armor Bar
        float armorY = statsY + barH + (isPortrait() ? 40f : 46f);
        textPaint.setColor(Color.rgb(200, 210, 200));
        textPaint.setTextSize(statLabelSize);
        drawIronblockText(canvas, "ARMOR", barX, armorY, textPaint);
        float armorVal = PLANE_HP[playerPlane];
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTextSize(Math.max(11f, viewHeight * 0.019f));
        drawIronblockText(canvas, String.valueOf(Math.round(armorVal)), barX + barW, armorY - barH * 0.20f, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
        drawStatBar(canvas, barX, armorY + 8f, barW, barH, armorVal / 200f, Color.rgb(255, 100, 100));

        // Unique trait display in a clean two-line block under stats.
        float traitTop = armorY + barH + (isPortrait() ? 20f : 26f);
        float traitH = isPortrait() ? viewHeight * 0.052f : viewHeight * 0.070f;
        rect.set(barX, traitTop, barX + barW, traitTop + traitH);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(90, 10, 16, 22));
        canvas.drawRoundRect(rect, 8f, 8f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.6f);
        paint.setColor(Color.argb(120, 210, 198, 156));
        canvas.drawRoundRect(rect, 8f, 8f, paint);
        paint.setStyle(Paint.Style.FILL);

        float traitPadX = Math.max(8f, viewWidth * 0.012f);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setColor(Color.rgb(240, 223, 176));
        textPaint.setTextSize(Math.max(11f, viewHeight * 0.0145f));
        drawIronblockText(canvas, "TRAIT: " + getPlaneTraitName(playerPlane), barX + traitPadX, traitTop + traitH * 0.38f, textPaint);
        textPaint.setColor(Color.rgb(214, 224, 224));
        textPaint.setTextSize(Math.max(10f, viewHeight * 0.0128f));
        drawIronblockText(canvas, getPlaneTraitDetail(playerPlane), barX + traitPadX, traitTop + traitH * 0.78f, textPaint);

        float selectorTop = getSelectorTop();
        drawButton(canvas, "<", getSelectorPrevLeft(), selectorTop, getSelectorPrevRight(), selectorTop + getSmallButtonHeight(), true, false);
        drawButton(canvas, ">", getSelectorNextLeft(), selectorTop, getSelectorNextRight(), selectorTop + getSmallButtonHeight(), true, false);

        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(getSettingsLabelTextSize());
        textPaint.setColor(Color.rgb(238, 236, 218));

        drawIronblockText(canvas, "GRAPHICS", getGraphicsStyleLeft(), getGraphicsStyleTop() - viewHeight * 0.022f, textPaint);
        drawButton(canvas, "FULL", getGraphicsFullLeft(), getGraphicsStyleTop(), getGraphicsFullRight(), getGraphicsStyleBottom(), true, !batterySaver);
        drawButton(canvas, "BATTERY SAVER", getGraphicsSaverLeft(), getGraphicsStyleTop(), getGraphicsSaverRight(), getGraphicsStyleBottom(), true, batterySaver);
        drawButton(canvas, "PRIVACY POLICY", getPrivacyButtonLeft(), getPrivacyButtonTop(), getPrivacyButtonRight(), getPrivacyButtonBottom(), true, false);
        
        if (popupActive) {
            drawPopup(canvas);
        }

        drawSlider(canvas, "MUSIC", SLIDER_MUSIC, musicVolume, getMusicSliderY());
        drawSlider(canvas, "SFX", SLIDER_SFX, sfxVolume, getSfxSliderY());
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawSlider(Canvas canvas, String label, int slider, float value, float y) {
        float left = getSliderLeft();
        float right = getSliderRight();
        float knobX = left + (right - left) * value;
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(getSettingsLabelTextSize());
        textPaint.setColor(Color.rgb(238, 236, 218));
        drawIronblockText(canvas, label, left, y - viewHeight * 0.024f, textPaint);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        drawIronblockText(canvas, slider == SLIDER_MUSIC ? musicPercentText : sfxPercentText, right, y - viewHeight * 0.024f, textPaint);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(isPortrait() ? Math.max(8f, viewWidth * 0.020f) : Math.max(9f, viewHeight * 0.020f));
        paint.setColor(Color.argb(116, 230, 230, 230));
        canvas.drawLine(left, y, right, y, paint);
        paint.setColor(slider == SLIDER_MUSIC ? Color.rgb(245, 188, 71) : Color.rgb(116, 203, 255));
        canvas.drawLine(left, y, knobX, y, paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
        canvas.drawCircle(knobX, y, isPortrait() ? clamp(viewWidth * 0.045f, 18f, 26f) : Math.max(17f, viewHeight * 0.035f), paint);
    }

    private void drawIronblockText(Canvas canvas, String text, float x, float y, Paint paint) {
        Paint.Style originalStyle = paint.getStyle();
        Shader originalShader = paint.getShader();
        int originalColor = paint.getColor();

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(paint.getTextSize() * 0.08f);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(Color.BLACK);
        canvas.drawText(text, x, y, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.clearShadowLayer();
        float textHeight = paint.getTextSize();
        LinearGradient metalShader = new LinearGradient(
                0, y - textHeight * 0.85f, 0, y,
                new int[]{Color.rgb(255, 238, 160), Color.rgb(220, 185, 80), Color.rgb(160, 115, 45), Color.rgb(245, 205, 95)},
                new float[]{0f, 0.38f, 0.62f, 1f},
                Shader.TileMode.CLAMP
        );
        paint.setShader(metalShader);
        canvas.drawText(text, x, y, paint);

        paint.setStyle(originalStyle);
        paint.setShader(originalShader);
        paint.setColor(originalColor);
        paint.setShadowLayer(4f, 2f, 2f, Color.argb(180, 0, 0, 0));
    }

    private boolean canDrawSpriteNumber(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9' || numberSprites[c - '0'] == null) {
                return false;
            }
        }
        return true;
    }

    private void drawSpriteNumberCentered(Canvas canvas, String value, float cx, float cy, float height, float gap) {
        if (!canDrawSpriteNumber(value)) {
            drawIronblockText(canvas, value, cx, cy, textPaint);
            return;
        }
        float totalW = 0f;
        for (int i = 0; i < value.length(); i++) {
            Bitmap digit = numberSprites[value.charAt(i) - '0'];
            totalW += height * ((float) digit.getWidth() / (float) digit.getHeight());
            if (i < value.length() - 1) {
                totalW += gap;
            }
        }
        float x = cx - totalW * 0.5f;
        for (int i = 0; i < value.length(); i++) {
            Bitmap digit = numberSprites[value.charAt(i) - '0'];
            float w = height * ((float) digit.getWidth() / (float) digit.getHeight());
            spriteRect.set(x, cy - height * 0.5f, x + w, cy + height * 0.5f);
            canvas.drawBitmap(digit, null, spriteRect, spritePaint);
            x += w + gap;
        }
    }

    private void drawSpriteNumberRightAligned(Canvas canvas, String value, float rightX, float cy, float height, float gap) {
        if (!canDrawSpriteNumber(value)) {
            drawIronblockText(canvas, value, rightX, cy, textPaint);
            return;
        }
        float totalW = 0f;
        for (int i = 0; i < value.length(); i++) {
            Bitmap digit = numberSprites[value.charAt(i) - '0'];
            totalW += height * ((float) digit.getWidth() / (float) digit.getHeight());
            if (i < value.length() - 1) {
                totalW += gap;
            }
        }
        float x = rightX - totalW;
        for (int i = 0; i < value.length(); i++) {
            Bitmap digit = numberSprites[value.charAt(i) - '0'];
            float w = height * ((float) digit.getWidth() / (float) digit.getHeight());
            spriteRect.set(x, cy - height * 0.5f, x + w, cy + height * 0.5f);
            canvas.drawBitmap(digit, null, spriteRect, spritePaint);
            x += w + gap;
        }
    }

    private float measureSpriteNumberWidth(String value, float height, float gap) {
        if (value == null || value.length() == 0) {
            return 0f;
        }
        if (!canDrawSpriteNumber(value)) {
            return textPaint.measureText(value);
        }
        float totalW = 0f;
        for (int i = 0; i < value.length(); i++) {
            Bitmap digit = numberSprites[value.charAt(i) - '0'];
            totalW += height * ((float) digit.getWidth() / (float) digit.getHeight());
            if (i < value.length() - 1) {
                totalW += gap;
            }
        }
        return totalW;
    }

    private void drawButton(Canvas canvas, String label, float left, float top, float right, float bottom, boolean enabled, boolean selected) {
        int fill = enabled
                ? (selected ? Color.argb(226, 219, 170, 72) : Color.argb(178, 36, 50, 61))
                : Color.argb(100, 70, 76, 81);
        rect.set(left, top, right, bottom);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fill);
        canvas.drawRoundRect(rect, 7f, 7f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        paint.setColor(enabled ? Color.argb(210, 245, 234, 193) : Color.argb(105, 220, 220, 220));
        canvas.drawRoundRect(rect, 7f, 7f, paint);
        paint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(boldTypeface);
        textPaint.setTextSize(fitTextSize(label, right - left, bottom - top));
        textPaint.setColor(enabled ? Color.WHITE : Color.argb(140, 235, 235, 235));
        textPaint.getFontMetrics(fontMetrics);
        drawIronblockText(canvas, label, (left + right) * 0.5f, (top + bottom) * 0.5f - (fontMetrics.ascent + fontMetrics.descent) * 0.5f, textPaint);
    }

    private float fitTextSize(String label, float width, float height) {
        float size = Math.max(12f, Math.min(23f, height * 0.42f));
        textPaint.setTextSize(size);
        while (size > 11f && textPaint.measureText(label) > width * 0.88f) {
            size -= 1f;
            textPaint.setTextSize(size);
        }
        return size;
    }

    private float fitDisplayTextSize(String label, float width, float maxSize, float minSize) {
        float size = maxSize;
        textPaint.setTextSize(size);
        while (size > minSize && textPaint.measureText(label) > width) {
            size -= 1f;
            textPaint.setTextSize(size);
        }
        return size;
    }

    private void drawPlayer(Canvas canvas) {
        float size = getPlayerSize();
        boolean hiddenByBlink = invulnerableTimer > 0f && ((int) (totalTime * 12f) % 2 == 0);
        if (!hiddenByBlink) {
            if (guardianTimer > 0f) {
                drawGuardianPlanes(canvas, size);
            }
            drawAircraft(canvas, playerX, playerY, size, playerTilt, true, playerPlane);
        }
        if (armorTimer > 0f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * 0.055f);
            paint.setColor(Color.argb(145, 137, 215, 255));
            canvas.drawCircle(playerX, playerY, size * 0.76f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        if (hitFlash > 0f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(size * 0.08f);
            paint.setColor(Color.argb((int) (170f * Math.min(1f, hitFlash * 3f)), 255, 248, 222));
            canvas.drawCircle(playerX, playerY, size * 0.66f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawGuardianPlanes(Canvas canvas, float playerSize) {
        float allySize = playerSize * 0.56f;
        float offsetX = playerSize * 1.38f;
        float allyY = playerY + playerSize * 0.20f;
        drawGuardianPlane(canvas, playerX - offsetX, allyY, allySize, playerTilt * 0.55f);
        drawGuardianPlane(canvas, playerX + offsetX, allyY, allySize, playerTilt * 0.55f);
    }

    private void drawGuardianPlane(Canvas canvas, float x, float y, float size, float rotation) {
        if (basicEnemySprite != null) {
            drawBitmapCentered(canvas, basicEnemySprite, x, y, size * 2.10f, 180f + rotation, spritePaint);
        } else {
            drawEnemyFighter(canvas, x, y, size, 180f + rotation, false);
        }
    }

    private void drawBitmapCentered(Canvas canvas, Bitmap bitmap, float cx, float cy, float targetHeight, float rotation, Paint bitmapPaint) {
        float aspect = bitmap.getWidth() / (float) Math.max(1, bitmap.getHeight());
        float width = targetHeight * aspect;
        spriteRect.set(cx - width * 0.5f, cy - targetHeight * 0.5f, cx + width * 0.5f, cy + targetHeight * 0.5f);
        canvas.save();
        canvas.rotate(rotation, cx, cy);
        canvas.drawBitmap(bitmap, null, spriteRect, bitmapPaint);
        canvas.restore();
    }

    private Bitmap getBitmap(Bitmap[] sprites, int index) {
        if (sprites == null || sprites.length == 0) {
            return null;
        }
        if (index < 0 || index >= sprites.length) {
            return null;
        }
        return sprites[index];
    }

    private void drawAircraft(Canvas canvas, float cx, float cy, float size, float rotation, boolean friendly, int planeType) {
        Bitmap sprite = getBitmap(playerAircraftSprites, planeType);
        if (sprite != null) {
            float targetHeight = size * (friendly ? 2.28f : 2.18f);
            drawBitmapCentered(canvas, sprite, cx, cy, targetHeight, rotation, spritePaint);
            return;
        }
        canvas.save();
        canvas.rotate(rotation, cx, cy);
        float s = size;
        paint.setStyle(Paint.Style.FILL);
        if (!friendly) {
            paint.setColor(Color.argb(65, 5, 9, 12));
            canvas.drawOval(cx - s * 0.70f, cy + s * 0.62f, cx + s * 0.70f, cy + s * 0.88f, paint);
        }

        boolean jet = PLANE_JET[planeType];
        int bodyColor = primaryColor;
        int wingColor = secondaryColor;
        int underside = lighten(primaryColor);

        if (jet) {
            path.reset();
            path.moveTo(cx, cy - s * 1.18f);
            path.lineTo(cx - s * 0.22f, cy - s * 0.60f);
            path.lineTo(cx - s * 0.18f, cy + s * 0.72f);
            path.lineTo(cx, cy + s * 1.02f);
            path.lineTo(cx + s * 0.18f, cy + s * 0.72f);
            path.lineTo(cx + s * 0.22f, cy - s * 0.60f);
            path.close();
            paint.setColor(bodyColor);
            canvas.drawPath(path, paint);

            paint.setColor(wingColor);
            path.reset();
            path.moveTo(cx - s * 0.10f, cy - s * 0.16f);
            path.lineTo(cx - s * 1.22f, cy + s * 0.38f);
            path.lineTo(cx - s * 1.04f, cy + s * 0.62f);
            path.lineTo(cx - s * 0.08f, cy + s * 0.26f);
            path.close();
            canvas.drawPath(path, paint);
            path.reset();
            path.moveTo(cx + s * 0.10f, cy - s * 0.16f);
            path.lineTo(cx + s * 1.22f, cy + s * 0.38f);
            path.lineTo(cx + s * 1.04f, cy + s * 0.62f);
            path.lineTo(cx + s * 0.08f, cy + s * 0.26f);
            path.close();
            canvas.drawPath(path, paint);

            paint.setColor(wingColor);
            path.reset();
            path.moveTo(cx - s * 0.12f, cy + s * 0.56f);
            path.lineTo(cx - s * 0.76f, cy + s * 0.90f);
            path.lineTo(cx - s * 0.18f, cy + s * 0.88f);
            path.close();
            canvas.drawPath(path, paint);
            path.reset();
            path.moveTo(cx + s * 0.12f, cy + s * 0.56f);
            path.lineTo(cx + s * 0.76f, cy + s * 0.90f);
            path.lineTo(cx + s * 0.18f, cy + s * 0.88f);
            path.close();
            canvas.drawPath(path, paint);

            paint.setColor(Color.rgb(30, 36, 38));
            canvas.drawOval(cx - s * 0.62f, cy + s * 0.36f, cx - s * 0.34f, cy + s * 0.72f, paint);
            canvas.drawOval(cx + s * 0.34f, cy + s * 0.36f, cx + s * 0.62f, cy + s * 0.72f, paint);
        } else {
            paint.setColor(wingColor);
            path.reset();
            path.moveTo(cx - s * 0.16f, cy - s * 0.14f);
            path.cubicTo(cx - s * 0.58f, cy - s * 0.04f, cx - s * 1.14f, cy + s * 0.10f, cx - s * 1.36f, cy + s * 0.30f);
            path.cubicTo(cx - s * 0.86f, cy + s * 0.40f, cx - s * 0.40f, cy + s * 0.36f, cx - s * 0.08f, cy + s * 0.14f);
            path.close();
            canvas.drawPath(path, paint);
            path.reset();
            path.moveTo(cx + s * 0.16f, cy - s * 0.14f);
            path.cubicTo(cx + s * 0.58f, cy - s * 0.04f, cx + s * 1.14f, cy + s * 0.10f, cx + s * 1.36f, cy + s * 0.30f);
            path.cubicTo(cx + s * 0.86f, cy + s * 0.40f, cx + s * 0.40f, cy + s * 0.36f, cx + s * 0.08f, cy + s * 0.14f);
            path.close();
            canvas.drawPath(path, paint);

            path.reset();
            path.moveTo(cx, cy - s * 1.16f);
            path.cubicTo(cx - s * 0.20f, cy - s * 0.70f, cx - s * 0.22f, cy + s * 0.50f, cx - s * 0.10f, cy + s * 0.88f);
            path.lineTo(cx, cy + s * 1.02f);
            path.lineTo(cx + s * 0.10f, cy + s * 0.88f);
            path.cubicTo(cx + s * 0.22f, cy + s * 0.50f, cx + s * 0.20f, cy - s * 0.70f, cx, cy - s * 1.16f);
            path.close();
            paint.setColor(bodyColor);
            canvas.drawPath(path, paint);

            paint.setColor(wingColor);
            path.reset();
            path.moveTo(cx - s * 0.10f, cy + s * 0.54f);
            path.lineTo(cx - s * 0.74f, cy + s * 0.78f);
            path.lineTo(cx - s * 0.17f, cy + s * 0.82f);
            path.close();
            canvas.drawPath(path, paint);
            path.reset();
            path.moveTo(cx + s * 0.10f, cy + s * 0.54f);
            path.lineTo(cx + s * 0.74f, cy + s * 0.78f);
            path.lineTo(cx + s * 0.17f, cy + s * 0.82f);
            path.close();
            canvas.drawPath(path, paint);

            paint.setColor(Color.argb(102, 236, 229, 198));
            canvas.drawOval(cx - s * 0.28f, cy - s * 1.22f, cx + s * 0.28f, cy - s * 0.82f, paint);
        }

        paint.setColor(underside);
        path.reset();
        path.moveTo(cx - s * 0.10f, cy - s * 0.38f);
        path.lineTo(cx - s * 0.10f, cy + s * 0.66f);
        path.lineTo(cx, cy + s * 0.86f);
        path.lineTo(cx + s * 0.10f, cy + s * 0.66f);
        path.lineTo(cx + s * 0.10f, cy - s * 0.38f);
        path.close();
        canvas.drawPath(path, paint);

        paint.setColor(Color.rgb(18, 28, 30));
        path.reset();
        path.moveTo(cx - s * 0.14f, cy - s * 0.50f);
        path.cubicTo(cx - s * 0.12f, cy - s * 0.72f, cx + s * 0.12f, cy - s * 0.72f, cx + s * 0.14f, cy - s * 0.50f);
        path.lineTo(cx + s * 0.10f, cy - s * 0.18f);
        path.lineTo(cx - s * 0.10f, cy - s * 0.18f);
        path.close();
        canvas.drawPath(path, paint);
        paint.setColor(Color.argb(210, 150, 207, 222));
        canvas.drawOval(cx - s * 0.12f, cy - s * 0.68f, cx + s * 0.12f, cy - s * 0.24f, paint);

        if (friendly) {
            drawFriendlyMark(canvas, cx - s * 0.58f, cy + s * 0.22f, s * 0.13f);
            drawFriendlyMark(canvas, cx + s * 0.58f, cy + s * 0.22f, s * 0.13f);
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.2f, s * 0.025f));
        paint.setColor(darken(bodyColor));
        canvas.drawLine(cx, cy - s * 0.96f, cx, cy + s * 0.78f, paint);
        canvas.drawLine(cx - s * 0.85f, cy + s * 0.26f, cx + s * 0.85f, cy + s * 0.26f, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.restore();
    }

    private void drawFriendlyMark(Canvas canvas, float cx, float cy, float radius) {
        paint.setColor(markingColor);
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setColor(Color.rgb(235, 231, 211));
        canvas.drawCircle(cx, cy, radius * 0.68f, paint);
        paint.setColor(Color.rgb(176, 54, 49));
        canvas.drawCircle(cx, cy, radius * 0.38f, paint);
    }

    private void drawEnemyFighter(Canvas canvas, float cx, float cy, float size, float rotation, boolean general) {
        Bitmap sprite = general ? generalEnemySprite : basicEnemySprite;
        if (sprite != null) {
            drawBitmapCentered(canvas, sprite, cx, cy, size * (general ? 2.05f : 1.96f), rotation, spritePaint);
            return;
        }
        canvas.save();
        canvas.rotate(180f + rotation, cx, cy);
        float s = size;
        int body = general ? Color.rgb(84, 92, 84) : Color.rgb(100, 109, 101);
        int wing = general ? Color.rgb(49, 63, 60) : Color.rgb(67, 82, 78);
        paint.setColor(Color.argb(58, 5, 9, 12));
        canvas.drawOval(cx - s * 0.70f, cy + s * 0.64f, cx + s * 0.70f, cy + s * 0.86f, paint);
        paint.setColor(wing);
        path.reset();
        path.moveTo(cx - s * 0.14f, cy - s * 0.08f);
        path.lineTo(cx - s * 1.22f, cy + s * 0.36f);
        path.lineTo(cx - s * 0.82f, cy + s * 0.54f);
        path.lineTo(cx - s * 0.08f, cy + s * 0.22f);
        path.close();
        canvas.drawPath(path, paint);
        path.reset();
        path.moveTo(cx + s * 0.14f, cy - s * 0.08f);
        path.lineTo(cx + s * 1.22f, cy + s * 0.36f);
        path.lineTo(cx + s * 0.82f, cy + s * 0.54f);
        path.lineTo(cx + s * 0.08f, cy + s * 0.22f);
        path.close();
        canvas.drawPath(path, paint);
        paint.setColor(body);
        path.reset();
        path.moveTo(cx, cy - s * 1.08f);
        path.lineTo(cx - s * 0.20f, cy + s * 0.72f);
        path.lineTo(cx, cy + s * 0.96f);
        path.lineTo(cx + s * 0.20f, cy + s * 0.72f);
        path.close();
        canvas.drawPath(path, paint);
        paint.setColor(wing);
        path.reset();
        path.moveTo(cx - s * 0.10f, cy + s * 0.55f);
        path.lineTo(cx - s * 0.58f, cy + s * 0.82f);
        path.lineTo(cx - s * 0.16f, cy + s * 0.78f);
        path.close();
        canvas.drawPath(path, paint);
        path.reset();
        path.moveTo(cx + s * 0.10f, cy + s * 0.55f);
        path.lineTo(cx + s * 0.58f, cy + s * 0.82f);
        path.lineTo(cx + s * 0.16f, cy + s * 0.78f);
        path.close();
        canvas.drawPath(path, paint);
        drawBalkanCross(canvas, cx - s * 0.52f, cy + s * 0.26f, s * 0.13f);
        drawBalkanCross(canvas, cx + s * 0.52f, cy + s * 0.26f, s * 0.13f);
        paint.setColor(Color.argb(210, 130, 188, 206));
        canvas.drawOval(cx - s * 0.10f, cy - s * 0.62f, cx + s * 0.10f, cy - s * 0.22f, paint);
        canvas.restore();
    }

    private void drawBossBomber(Canvas canvas, Enemy boss) {
        Bitmap sprite = getBitmap(bossAircraftSprites, boss.spriteIndex);
        if (sprite != null) {
            drawBitmapCentered(canvas, sprite, boss.x, boss.y, boss.size * 2.16f, 0f, spritePaint);
            return;
        }
        canvas.save();
        float cx = boss.x;
        float cy = boss.y;
        float s = boss.size;
        canvas.rotate(180f, cx, cy);
        paint.setColor(Color.argb(72, 5, 9, 12));
        canvas.drawOval(cx - s * 1.05f, cy + s * 0.72f, cx + s * 1.05f, cy + s * 0.98f, paint);
        paint.setColor(Color.rgb(73, 84, 78));
        path.reset();
        path.moveTo(cx - s * 0.20f, cy - s * 0.10f);
        path.lineTo(cx - s * 1.75f, cy + s * 0.22f);
        path.lineTo(cx - s * 1.48f, cy + s * 0.56f);
        path.lineTo(cx - s * 0.12f, cy + s * 0.26f);
        path.close();
        canvas.drawPath(path, paint);
        path.reset();
        path.moveTo(cx + s * 0.20f, cy - s * 0.10f);
        path.lineTo(cx + s * 1.75f, cy + s * 0.22f);
        path.lineTo(cx + s * 1.48f, cy + s * 0.56f);
        path.lineTo(cx + s * 0.12f, cy + s * 0.26f);
        path.close();
        canvas.drawPath(path, paint);
        paint.setColor(Color.rgb(91, 101, 93));
        path.reset();
        path.moveTo(cx, cy - s * 1.05f);
        path.cubicTo(cx - s * 0.28f, cy - s * 0.54f, cx - s * 0.30f, cy + s * 0.66f, cx, cy + s * 1.05f);
        path.cubicTo(cx + s * 0.30f, cy + s * 0.66f, cx + s * 0.28f, cy - s * 0.54f, cx, cy - s * 1.05f);
        path.close();
        canvas.drawPath(path, paint);
        paint.setColor(Color.rgb(36, 45, 45));
        canvas.drawOval(cx - s * 0.86f, cy + s * 0.24f, cx - s * 0.52f, cy + s * 0.66f, paint);
        canvas.drawOval(cx + s * 0.52f, cy + s * 0.24f, cx + s * 0.86f, cy + s * 0.66f, paint);
        drawBalkanCross(canvas, cx - s * 0.96f, cy + s * 0.34f, s * 0.16f);
        drawBalkanCross(canvas, cx + s * 0.96f, cy + s * 0.34f, s * 0.16f);
        paint.setColor(Color.argb(210, 140, 190, 207));
        canvas.drawOval(cx - s * 0.16f, cy - s * 0.72f, cx + s * 0.16f, cy - s * 0.28f, paint);
        canvas.restore();
    }

    private void drawBalkanCross(Canvas canvas, float cx, float cy, float r) {
        paint.setColor(Color.rgb(236, 232, 218));
        rect.set(cx - r, cy - r * 0.28f, cx + r, cy + r * 0.28f);
        canvas.drawRect(rect, paint);
        rect.set(cx - r * 0.28f, cy - r, cx + r * 0.28f, cy + r);
        canvas.drawRect(rect, paint);
        paint.setColor(Color.rgb(20, 23, 24));
        rect.set(cx - r * 0.72f, cy - r * 0.14f, cx + r * 0.72f, cy + r * 0.14f);
        canvas.drawRect(rect, paint);
        rect.set(cx - r * 0.14f, cy - r * 0.72f, cx + r * 0.14f, cy + r * 0.72f);
        canvas.drawRect(rect, paint);
    }

    private void drawEnemies(Canvas canvas) {
        for (Enemy enemy : enemies) {
            if (enemy.dead) continue;
            if (enemy.type == TIER_BOSS) {
                drawBossBomber(canvas, enemy);
            } else {
                drawEnemyFighter(canvas, enemy.x, enemy.y, enemy.size, 0f, enemy.type == TIER_GENERAL);
            }
            if (enemy.hitTimer > 0f) {
                paint.setColor(Color.argb((int) (185f * Math.min(1f, enemy.hitTimer * 6f)), 255, 255, 240));
                canvas.drawCircle(enemy.x, enemy.y, getEnemyRadius(enemy) * 0.82f, paint);
            }
            drawEnemyHpBar(canvas, enemy);
        }
    }

    private void drawEnemyHpBar(Canvas canvas, Enemy enemy) {
        float width = enemy.type == TIER_BOSS ? Math.min(viewWidth * 0.42f, enemy.size * 2.1f) : enemy.size * 1.05f;
        float height = Math.max(6f, viewHeight * 0.010f);
        float x = enemy.x - width * 0.5f;
        float y = enemy.y - getEnemyRadius(enemy) * (enemy.type == TIER_BOSS ? 0.86f : 0.78f);
        paint.setColor(Color.argb(155, 8, 14, 18));
        rect.set(x, y, x + width, y + height);
        canvas.drawRoundRect(rect, 4f, 4f, paint);
        paint.setColor(enemy.type == TIER_BOSS ? Color.rgb(225, 76, 55) : enemy.type == TIER_GENERAL ? Color.rgb(234, 203, 75) : Color.rgb(135, 220, 124));
        rect.set(x, y, x + width * clamp(enemy.hp / enemy.maxHp, 0f, 1f), y + height);
        canvas.drawRoundRect(rect, 4f, 4f, paint);
    }

    private void drawBossWeapons(Canvas canvas) {
        for (Enemy enemy : enemies) {
            if (enemy.type != TIER_BOSS || enemy.dead || (enemy.laserWarmup <= 0f && enemy.laserTimer <= 0f)) {
                continue;
            }
            float beamTop = enemy.y + getEnemyRadius(enemy) * 0.48f;
            float halfWidth = enemy.laserWidth * 0.5f;
            if (enemy.laserWarmup > 0f) {
                float pulse = 0.45f + 0.55f * (float) Math.sin(totalTime * 22f);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeWidth(Math.max(4f, halfWidth * 0.10f));
                paint.setColor(Color.argb((int) (95f + pulse * 95f), 255, 64, 45));
                canvas.drawLine(enemy.laserX, beamTop, enemy.laserX, viewHeight, paint);
                paint.setStrokeWidth(Math.max(2f, halfWidth * 0.055f));
                paint.setColor(Color.argb(210, 255, 229, 149));
                canvas.drawLine(enemy.laserX - halfWidth, beamTop, enemy.laserX - halfWidth, viewHeight, paint);
                canvas.drawLine(enemy.laserX + halfWidth, beamTop, enemy.laserX + halfWidth, viewHeight, paint);
                paint.setStrokeCap(Paint.Cap.BUTT);
            } else {
                glowPaint.setColor(Color.argb(155, 255, 72, 44));
                canvas.drawRect(enemy.laserX - enemy.laserWidth, beamTop, enemy.laserX + enemy.laserWidth, viewHeight, glowPaint);
                paint.setColor(Color.argb(110, 255, 88, 46));
                canvas.drawRect(enemy.laserX - halfWidth * 1.55f, beamTop, enemy.laserX + halfWidth * 1.55f, viewHeight, paint);
                paint.setColor(Color.argb(226, 255, 226, 172));
                canvas.drawRect(enemy.laserX - halfWidth, beamTop, enemy.laserX + halfWidth, viewHeight, paint);
            }
        }
    }

    private void drawBullets(Canvas canvas) {
        for (Bullet bullet : bullets) {
            int color = bullet.rocket ? Color.rgb(255, 128, 42) : Color.rgb(248, 226, 119);
            glowPaint.setColor(Color.argb(bullet.rocket ? 132 : 92, Color.red(color), Color.green(color), Color.blue(color)));
            canvas.drawCircle(bullet.x, bullet.y, bullet.radius * (bullet.rocket ? 2.6f : 1.8f), glowPaint);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(bullet.rocket ? 5f : 3f);
            paint.setColor(Color.argb(165, 255, 245, 185));
            canvas.drawLine(bullet.x, bullet.y + bullet.radius * (bullet.rocket ? 5.5f : 3.8f), bullet.x, bullet.y, paint);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setColor(color);
            canvas.drawCircle(bullet.x, bullet.y, bullet.radius, paint);
        }
    }

    private void drawEnemyShots(Canvas canvas) {
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(4f);
        for (EnemyShot shot : enemyShots) {
            glowPaint.setColor(Color.argb(100, 255, 67, 48));
            canvas.drawCircle(shot.x, shot.y, 13f, glowPaint);
            paint.setColor(Color.rgb(255, 86, 59));
            canvas.drawLine(shot.x, shot.y - 13f, shot.x, shot.y + 8f, paint);
        }
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawPickups(Canvas canvas) {
        for (Pickup pickup : pickups) {
            float r = Math.max(16f, Math.min(viewWidth, viewHeight) * 0.026f);
            if (pickup.type == POWER_GOLD) {
                drawBitmapCentered(canvas, goldCoinSprite, pickup.x, pickup.y, r * 1.8f, pickup.spin * 0.12f, spritePaint);
                continue;
            }
            Bitmap icon = getBitmap(powerupSprites, pickup.type);
            if (icon != null) {
                drawBitmapCentered(canvas, icon, pickup.x, pickup.y, r * 2.05f, pickup.spin * 0.12f, spritePaint);
                continue;
            }
            canvas.save();
            canvas.rotate(pickup.spin, pickup.x, pickup.y);
            paint.setColor(getPowerColor(pickup.type));
            path.reset();
            path.moveTo(pickup.x, pickup.y - r);
            path.lineTo(pickup.x + r, pickup.y);
            path.lineTo(pickup.x, pickup.y + r);
            path.lineTo(pickup.x - r, pickup.y);
            path.close();
            canvas.drawPath(path, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f);
            paint.setColor(Color.argb(220, 255, 255, 235));
            canvas.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.restore();
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(Math.max(13f, r * 0.72f));
            textPaint.setColor(Color.rgb(20, 25, 28));
            drawIronblockText(canvas, getPowerLabel(pickup.type), pickup.x, pickup.y + r * 0.28f, textPaint);
            textPaint.setColor(Color.WHITE);
        }
    }

    private void drawPickupTexts(Canvas canvas) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(boldTypeface);
        for (PickupText pickupText : pickupTexts) {
            float t = clamp(pickupText.age / Math.max(0.01f, pickupText.life), 0f, 1f);
            int alpha = (int) (255f * (1f - Math.max(0f, t - 0.55f) / 0.45f));
            textPaint.setTextSize(Math.max(12f, viewHeight * 0.024f) * (1f + (1f - t) * 0.10f));
            textPaint.setColor(Color.argb(alpha, 255, 234, 150));
            textPaint.setShadowLayer(5f, 0f, 2f, Color.argb((int) (180f * alpha / 255f), 0, 0, 0));
            drawIronblockText(canvas, pickupText.text, pickupText.x, pickupText.y, textPaint);
        }
        textPaint.setShadowLayer(4f, 0f, 2f, Color.argb(190, 0, 0, 0));
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawStatBar(Canvas canvas, float x, float y, float w, float h, float pct, int barColor) {
        rect.set(x, y, x + w, y + h);
        paint.setColor(Color.argb(80, 0, 0, 0));
        canvas.drawRect(rect, paint);
        
        float fillW = w * clamp(pct, 0f, 1f);
        if (fillW > 0) {
            rect.set(x, y, x + fillW, y + h);
            paint.setColor(barColor);
            canvas.drawRect(rect, paint);
        }
        
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        paint.setColor(Color.argb(120, 200, 200, 200));
        rect.set(x, y, x + w, y + h);
        canvas.drawRect(rect, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawExplosions(Canvas canvas) {
        for (Explosion explosion : explosions) {
            float t = clamp(explosion.age / explosion.life, 0f, 1f);
            int red = Color.red(explosion.color);
            int green = Color.green(explosion.color);
            int blue = Color.blue(explosion.color);
            if (explosion.smoke) {
                float radius = explosion.radius * (0.55f + t * 1.65f);
                int alpha = (int) (Color.alpha(explosion.color) * square(1f - t));
                paint.setColor(Color.argb(alpha, red, green, blue));
                canvas.drawOval(explosion.x - radius * 1.35f, explosion.y - radius * 0.80f, explosion.x + radius * 1.35f, explosion.y + radius * 0.95f, paint);
                paint.setColor(Color.argb((int) (alpha * 0.45f), 25, 24, 23));
                canvas.drawOval(explosion.x - radius * 0.95f, explosion.y - radius * 0.55f, explosion.x + radius * 0.65f, explosion.y + radius * 0.70f, paint);
                continue;
            }
            float radius = explosion.radius * (0.35f + t * 1.35f);
            int alpha = (int) (190f * (1f - t));
            glowPaint.setColor(Color.argb(alpha, red, green, blue));
            canvas.drawCircle(explosion.x, explosion.y, radius * 1.15f, glowPaint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2f, radius * 0.08f));
            paint.setColor(Color.argb(alpha, red, green, blue));
            canvas.drawCircle(explosion.x, explosion.y, radius, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawLaser(Canvas canvas) {
        float s = getPlayerSize();
        float beamWidth = s * (0.74f + (batterySaver ? 0f : 0.08f * (float) Math.sin(totalTime * 38f)));
        float left = playerX - beamWidth * 0.5f;
        float right = playerX + beamWidth * 0.5f;
        float bottom = playerY - s * 0.55f;
        glowPaint.setColor(Color.argb(140, 255, 218, 92));
        canvas.drawRect(left - beamWidth * 0.9f, -20f, right + beamWidth * 0.9f, bottom, glowPaint);
        paint.setColor(Color.argb(210, 255, 252, 214));
        canvas.drawRect(left, -20f, right, bottom, paint);
    }

    private void drawHud(Canvas canvas) {
        updateHudTextCache();
        float margin = isPortrait() ? Math.max(12f, viewWidth * 0.055f) : Math.max(16f, viewHeight * 0.040f);
        float hpWidth = isPortrait() ? Math.min(viewWidth * 0.52f, 320f) : Math.min(viewWidth * 0.55f, 500f);
        float hpHeight = isPortrait() ? clamp(viewWidth * 0.065f, 22f, 32f) : Math.max(24f, viewHeight * 0.052f);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(isPortrait() ? clamp(viewWidth * 0.054f, 22f, 28f) : Math.max(20f, viewHeight * 0.038f));
        textPaint.setColor(Color.WHITE);
        drawIronblockText(canvas, scoreText, margin, margin + textPaint.getTextSize(), textPaint);
        drawIronblockText(canvas, waveText, margin, margin + textPaint.getTextSize() * (isPortrait() ? 1.95f : 2.15f), textPaint);
        drawIronblockText(canvas, livesText, margin, margin + textPaint.getTextSize() * (isPortrait() ? 2.90f : 3.30f), textPaint);
        
        // Draw Gold (compact so up to 3 digits stays inside safe area)
        textPaint.setTextAlign(Paint.Align.RIGHT);
        float hudGoldTextSize = textPaint.getTextSize() * 0.82f;
        textPaint.setTextSize(hudGoldTextSize);
        float goldX = viewWidth - margin - (isPortrait() ? viewWidth * 0.215f : viewWidth * 0.215f);
        float goldY = margin + hudGoldTextSize;
        drawIronblockText(canvas, "GOLD " + gold, goldX, goldY, textPaint);
        float iconSize = hudGoldTextSize * 0.92f;
        float textWidth = textPaint.measureText("GOLD " + gold);
        spriteRect.set(goldX - textWidth - iconSize - 10f, goldY - iconSize * 0.85f, goldX - textWidth - 10f, goldY + iconSize * 0.15f);
        canvas.drawBitmap(goldCoinSprite, null, spriteRect, spritePaint);
        textPaint.setTextSize(isPortrait() ? clamp(viewWidth * 0.054f, 22f, 28f) : Math.max(20f, viewHeight * 0.038f));
        textPaint.setTextAlign(Paint.Align.LEFT);
        float barY = margin + textPaint.getTextSize() * (isPortrait() ? 3.35f : 4.05f);
        paint.setColor(Color.argb(160, 8, 14, 18));
        rect.set(margin, barY, margin + hpWidth, barY + hpHeight);
        canvas.drawRoundRect(rect, 5f, 5f, paint);
        float hpPct = clamp(playerHp / Math.max(1f, getPlayerMaxHp()), 0f, 1f);
        paint.setColor(hpPct > 0.45f ? Color.rgb(95, 214, 112) : hpPct > 0.22f ? Color.rgb(242, 196, 68) : Color.rgb(231, 75, 55));
        rect.set(margin, barY, margin + hpWidth * hpPct, barY + hpHeight);
        canvas.drawRoundRect(rect, 5f, 5f, paint);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(isPortrait() ? clamp(viewWidth * 0.030f, 11f, 16f) : Math.max(12f, hpHeight * 0.82f));
        textPaint.setColor(Color.WHITE);
        drawIronblockText(canvas, hpText, margin + hpWidth * 0.5f, barY + hpHeight * 0.84f, textPaint);

        if (gameScreen == SCREEN_PLAYING && !gameOver) {
            float size = getTopButtonSize();
            float left = getGameplayPauseButtonLeft();
            float top = getGameplayPauseButtonTop();
            drawButton(canvas, "II", left, top, left + size, top + size, true, false);
        }
        drawPerformanceCounters(canvas);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawPerformanceCounters(Canvas canvas) {
        if (!SHOW_DEBUG_COUNTERS) {
            return;
        }
        float margin = Math.max(16f, viewHeight * 0.030f);
        float x = viewWidth - margin;
        float y = getGameplayPauseButtonTop() + getTopButtonSize() + margin * 1.15f;
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTypeface(boldTypeface);
        textPaint.setTextSize(Math.max(10f, viewHeight * 0.022f));
        textPaint.setColor(Color.argb(185, 235, 242, 235));
        drawIronblockText(canvas, debugLineOne, x, y, textPaint);
        drawIronblockText(canvas, debugLineTwo, x, y + textPaint.getTextSize() * 1.15f, textPaint);
        if (batterySaver) {
            textPaint.setColor(Color.argb(170, 255, 224, 130));
            drawIronblockText(canvas, "BATTERY SAVER", x, y + textPaint.getTextSize() * 2.30f, textPaint);
        }
    }

    private void drawControls(Canvas canvas) {
        float r = getJoystickRadius();
        float cx = getJoystickCenterX();
        float cy = getJoystickCenterY();
        if (controlStyle == CONTROL_FIXED) {
            paint.setColor(Color.argb(58, 255, 255, 255));
            canvas.drawCircle(cx, cy, r, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(3f, r * 0.06f));
            paint.setColor(Color.argb(150, 235, 232, 208));
            canvas.drawCircle(cx, cy, r, paint);
            paint.setStyle(Paint.Style.FILL);
            float knobX = cx + joystickX * r * 0.58f;
            float knobY = cy + joystickY * r * 0.58f;
            paint.setColor(joystickPointerId != INVALID_POINTER_ID ? Color.argb(190, 248, 222, 145) : Color.argb(135, 238, 232, 206));
            canvas.drawCircle(knobX, knobY, r * 0.34f, paint);
        }

        float br = getSpecialButtonRadius();
        float bx = getSpecialButtonCenterX();
        float by = getSpecialButtonCenterY();
        boolean ready = specialCharges > 0 && laserTimer <= 0f;
        boolean pressed = specialPointerId != INVALID_POINTER_ID;
        paint.setColor(ready ? Color.argb(pressed ? 220 : 178, 245, 188, 71) : Color.argb(86, 145, 151, 158));
        canvas.drawCircle(bx, by, pressed ? br * 0.94f : br, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(3f, br * 0.07f));
        paint.setColor(ready ? Color.argb(220, 255, 242, 190) : Color.argb(130, 230, 230, 230));
        canvas.drawCircle(bx, by, br, paint);
        paint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(boldTypeface);
        textPaint.setTextSize(Math.max(18f, br * 0.40f));
        textPaint.setColor(ready ? Color.rgb(47, 35, 16) : Color.argb(180, 40, 43, 47));
        drawIronblockText(canvas, "LASER", bx, by + br * 0.06f, textPaint);
        textPaint.setTextSize(Math.max(12f, br * 0.27f));
        drawIronblockText(canvas, specialChargeText, bx, by + br * 0.54f, textPaint);

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

    }

    private void drawBanner(Canvas canvas) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(boldTypeface);
        textPaint.setTextSize(Math.max(22f, viewHeight * 0.042f));
        float alpha = Math.min(1f, bannerTimer);
        textPaint.setColor(Color.argb((int) (255f * alpha), 255, 238, 184));
        drawIronblockText(canvas, bannerText, viewWidth * 0.50f, viewHeight * 0.18f, textPaint);
        textPaint.setColor(Color.WHITE);
    }

    private void drawGameOver(Canvas canvas) {
        paint.setColor(Color.argb(136, 0, 0, 0));
        canvas.drawRect(0f, 0f, viewWidth, viewHeight, paint);
        drawConfetti(canvas);
        
        // Game Over Banner
        float bannerW = Math.min(viewWidth * 0.84f, 720f);
        float bannerH = bannerW * 0.56f;
        spriteRect.set(viewWidth * 0.50f - bannerW * 0.50f, viewHeight * 0.22f - bannerH * 0.50f, viewWidth * 0.50f + bannerW * 0.50f, viewHeight * 0.22f + bannerH * 0.50f);
        canvas.drawBitmap(gameOverSprite, null, spriteRect, spritePaint);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(boldTypeface);
        
        // Score section
        textPaint.setTextSize(Math.max(20f, viewHeight * 0.045f));
        textPaint.setColor(Color.rgb(255, 221, 162));
        float sectionLabelH = viewHeight * 0.043f;
        if (scoreLabelSprite != null) {
            drawBitmapCentered(canvas, scoreLabelSprite, viewWidth * 0.50f, viewHeight * 0.365f, sectionLabelH, 0f, spritePaint);
        } else {
            drawIronblockText(canvas, "SCORE", viewWidth * 0.50f, viewHeight * 0.38f, textPaint);
        }
        
        textPaint.setTextSize(newHighScore ? Math.max(28f, viewHeight * 0.058f) : Math.max(24f, viewHeight * 0.052f));
        textPaint.setColor(newHighScore ? Color.rgb(255, 238, 138) : Color.WHITE);
        drawSpriteNumberCentered(canvas, String.valueOf(score), viewWidth * 0.50f, viewHeight * 0.420f, viewHeight * 0.048f, Math.max(2f, viewWidth * 0.005f));
        
        if (!newHighScore && highScore > 0) {
            if (bestScoreLabelSprite != null) {
                drawBitmapCentered(canvas, bestScoreLabelSprite, viewWidth * 0.50f, viewHeight * 0.485f, sectionLabelH, 0f, spritePaint);
            } else {
                textPaint.setTextSize(Math.max(14f, viewHeight * 0.032f));
                textPaint.setColor(Color.rgb(218, 224, 221));
                drawIronblockText(canvas, "BEST SCORE", viewWidth * 0.50f, viewHeight * 0.49f, textPaint);
            }
            drawSpriteNumberCentered(canvas, String.valueOf(highScore), viewWidth * 0.50f, viewHeight * 0.530f, viewHeight * 0.038f, Math.max(2f, viewWidth * 0.0045f));
        }

        // Gold section
        textPaint.setTextSize(Math.max(18f, viewHeight * 0.042f));
        textPaint.setColor(Color.rgb(255, 215, 0));
        if (goldObtainedLabelSprite != null) {
            drawBitmapCentered(canvas, goldObtainedLabelSprite, viewWidth * 0.50f, viewHeight * 0.600f, sectionLabelH, 0f, spritePaint);
        } else {
            drawIronblockText(canvas, "GOLD OBTAINED", viewWidth * 0.50f, viewHeight * 0.56f, textPaint);
        }
        
        String goldValStr = String.valueOf(goldEarned);
        float coinSize = textPaint.getTextSize() * 1.20f;
        float goldValY = viewHeight * 0.655f;
        float digitH = coinSize * 1.10f;
        float digitsW = 0f;
        if (canDrawSpriteNumber(goldValStr)) {
            for (int i = 0; i < goldValStr.length(); i++) {
                Bitmap digit = numberSprites[goldValStr.charAt(i) - '0'];
                digitsW += digitH * ((float) digit.getWidth() / (float) digit.getHeight());
                if (i < goldValStr.length() - 1) digitsW += Math.max(2f, digitH * 0.08f);
            }
        } else {
            digitsW = textPaint.measureText(goldValStr);
        }
        float startX = viewWidth * 0.50f - (coinSize + 12f + digitsW) * 0.5f;
        spriteRect.set(startX, goldValY - coinSize * 0.50f, startX + coinSize, goldValY + coinSize * 0.50f);
        canvas.drawBitmap(goldCoinSprite, null, spriteRect, spritePaint);
        drawSpriteNumberCentered(canvas, goldValStr, startX + coinSize + 12f + digitsW * 0.5f, goldValY, digitH * 0.82f, Math.max(2f, digitH * 0.08f));

        // Revive Button
        if (!hasRevived) {
            float btnW = Math.min(viewWidth * 0.65f, 520f);
            float btnH = btnW * 0.26f;
            float btnX = viewWidth * 0.50f - btnW * 0.50f;
            float btnY = getReviveButtonTopY();
            spriteRect.set(btnX, btnY, btnX + btnW, btnY + btnH);
            canvas.drawBitmap(reviveSprite, null, spriteRect, spritePaint);
        }

        // Buttons
        float buttonX = viewWidth * 0.5f;
        float bh = getEndMenuButtonHeight();
        float bw = getEndMenuButtonWidth();
        float restartY = getEndRestartButtonCenterY();
        float mainMenuY = getEndMainMenuButtonCenterY();
        float endButtonScale = bh * 0.92f;

        if (restartSprite != null) {
            drawBitmapCentered(canvas, restartSprite, buttonX, restartY, endButtonScale, 0f, spritePaint);
        } else {
            drawButton(canvas, "RESTART", buttonX - bw * 0.5f, restartY - bh * 0.5f, buttonX + bw * 0.5f, restartY + bh * 0.5f, true, false);
        }

        if (mainMenuSprite != null) {
            drawBitmapCentered(canvas, mainMenuSprite, buttonX, mainMenuY, endButtonScale, 0f, spritePaint);
        } else {
            drawButton(canvas, "MAIN MENU", buttonX - bw * 0.5f, mainMenuY - bh * 0.5f, buttonX + bw * 0.5f, mainMenuY + bh * 0.5f, true, false);
        }
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    
    private void triggerAtomicBomb() {
        audio.playAtomicBomb();
        for (int i = enemies.size() - 1; i >= 0; i--) {
            destroyEnemy(enemies.get(i));
        }
        enemyShots.clear();
        bannerText = "ATOMIC DETONATION!";
        bannerTimer = 1.8f;
    }

    private void drawConfetti(Canvas canvas) {
        for (Confetti piece : confetti) {
            float t = clamp(piece.age / Math.max(0.01f, piece.life), 0f, 1f);
            paint.setColor(Color.argb((int) (230f * (1f - t)), Color.red(piece.color), Color.green(piece.color), Color.blue(piece.color)));
            canvas.save();
            canvas.rotate(piece.rotation, piece.x, piece.y);
            rect.set(piece.x - piece.size * 0.45f, piece.y - piece.size * 0.16f, piece.x + piece.size * 0.45f, piece.y + piece.size * 0.16f);
            canvas.drawRoundRect(rect, 2f, 2f, paint);
            canvas.restore();
        }
    }

    private void spawnHighScoreConfetti() {
        confetti.clear();
        int[] colors = {
                Color.rgb(255, 222, 84),
                Color.rgb(255, 92, 82),
                Color.rgb(101, 211, 255),
                Color.rgb(135, 238, 142),
                Color.rgb(248, 159, 255)
        };
        int count = getMaxConfetti();
        for (int i = 0; i < count; i++) {
            Confetti piece = new Confetti();
            piece.x = random.nextFloat() * Math.max(1f, viewWidth);
            piece.y = -random.nextFloat() * viewHeight * 0.75f;
            piece.vx = (random.nextFloat() - 0.5f) * 185f;
            piece.vy = 90f + random.nextFloat() * 180f;
            piece.size = Math.max(8f, viewHeight * (0.014f + random.nextFloat() * 0.014f));
            piece.rotation = random.nextFloat() * 360f;
            piece.spin = (random.nextFloat() - 0.5f) * 720f;
            piece.life = 2.6f + random.nextFloat() * 1.8f;
            piece.color = colors[random.nextInt(colors.length)];
            confetti.add(piece);
        }
    }

    private void addExplosion(float x, float y, float radius, int color) {
        addExplosion(x, y, radius, color, 0.55f);
    }

    private void addExplosion(float x, float y, float radius, int color, float life) {
        if (getActiveExplosionCount() >= getMaxExplosions()) {
            return;
        }
        Explosion explosion = new Explosion();
        explosion.x = x;
        explosion.y = y;
        explosion.radius = radius;
        explosion.color = color;
        explosion.life = life;
        explosions.add(explosion);
    }

    private void addSmokePuff(float x, float y, float radius, int color, float vx, float vy, float life) {
        if (getActiveSmokeCount() >= getMaxSmokeParticles()) {
            return;
        }
        Explosion smoke = new Explosion();
        smoke.x = x;
        smoke.y = y;
        smoke.radius = radius;
        smoke.color = color;
        smoke.vx = vx;
        smoke.vy = vy;
        smoke.life = life;
        smoke.smoke = true;
        explosions.add(smoke);
    }

    private int getMaxBullets() {
        return batterySaver ? MAX_BULLETS_BATTERY : MAX_BULLETS;
    }

    private int getMaxEnemyShots() {
        return batterySaver ? MAX_ENEMY_SHOTS_BATTERY : MAX_ENEMY_SHOTS;
    }

    private int getMaxExplosions() {
        return batterySaver ? MAX_EXPLOSIONS_BATTERY : MAX_EXPLOSIONS;
    }

    private int getMaxSmokeParticles() {
        return batterySaver ? MAX_SMOKE_PARTICLES_BATTERY : MAX_SMOKE_PARTICLES;
    }

    private int getMaxConfetti() {
        return batterySaver ? MAX_CONFETTI_BATTERY : MAX_CONFETTI;
    }

    private int getMaxClouds() {
        return batterySaver ? MAX_CLOUDS_BATTERY : MAX_CLOUDS;
    }

    private boolean isEnemyShotPressureHigh() {
        return enemyShots.size() >= getMaxEnemyShots() * 0.72f;
    }

    private int getActiveExplosionCount() {
        int count = 0;
        for (Explosion explosion : explosions) {
            if (!explosion.smoke) {
                count++;
            }
        }
        return count;
    }

    private int getActiveSmokeCount() {
        int count = 0;
        for (Explosion explosion : explosions) {
            if (explosion.smoke) {
                count++;
            }
        }
        return count;
    }

    private int getParticleCount() {
        return getActiveSmokeCount() + confetti.size() + pickupTexts.size();
    }

    private void trimVisualEffectsToCaps() {
        trimBulletsToCap();
        while (enemyShots.size() > getMaxEnemyShots()) {
            enemyShots.remove(0);
        }
        while (confetti.size() > getMaxConfetti()) {
            confetti.remove(0);
        }
        while (pickupTexts.size() > MAX_PICKUP_TEXTS) {
            pickupTexts.remove(0);
        }
        trimExplosionsToCaps();
    }

    private void trimBulletsToCap() {
        while (bullets.size() > getMaxBullets()) {
            bullets.remove(0);
        }
    }

    private void trimExplosionsToCaps() {
        while (getActiveExplosionCount() > getMaxExplosions()) {
            removeOldestExplosion(false);
        }
        while (getActiveSmokeCount() > getMaxSmokeParticles()) {
            removeOldestExplosion(true);
        }
    }

    private void removeOldestExplosion(boolean smoke) {
        for (int i = 0; i < explosions.size(); i++) {
            if (explosions.get(i).smoke == smoke) {
                explosions.remove(i);
                return;
            }
        }
    }

    private float getPlayerSize() {
        return Math.max(55f, Math.min(viewWidth, viewHeight) * 0.112f);
    }

    private float getPlayerMaxHp() {
        return PLANE_HP[playerPlane];
    }

    private float getEnemyRadius(Enemy enemy) {
        return enemy.size * (enemy.type == TIER_BOSS ? 0.60f : enemy.type == TIER_GENERAL ? 0.56f : 0.52f);
    }

    private float getGunInterval() {
        if (gunPowerTimer > 0f && barrageTimer > 0f) {
            return 0.104f * PLANE_FIRE_INTERVAL_MULT[playerPlane];
        }
        if (gunPowerTimer > 0f) {
            return 0.112f * PLANE_FIRE_INTERVAL_MULT[playerPlane];
        }
        return (barrageTimer > 0f ? 0.135f : 0.185f) * PLANE_FIRE_INTERVAL_MULT[playerPlane];
    }

    private int getPowerColor(int type) {
        if (type == POWER_GUN) {
            return Color.rgb(116, 203, 255);
        }
        if (type == POWER_ROCKET) {
            return Color.rgb(255, 128, 42);
        }
        if (type == POWER_ARMOR) {
            return Color.rgb(137, 215, 255);
        }
        if (type == POWER_BARRAGE) {
            return Color.rgb(255, 195, 74);
        }
        if (type == POWER_GUARDIAN) {
            return Color.rgb(255, 230, 146);
        }
        if (type == POWER_SPECIAL) {
            return Color.rgb(245, 188, 71);
        }
        return Color.rgb(99, 224, 141);
    }

    private String getPowerLabel(int type) {
        if (type == POWER_GUN) {
            return "G";
        }
        if (type == POWER_ROCKET) {
            return "R";
        }
        if (type == POWER_ARMOR) {
            return "A";
        }
        if (type == POWER_BARRAGE) {
            return "B";
        }
        if (type == POWER_GUARDIAN) {
            return "G";
        }
        if (type == POWER_SPECIAL) {
            return "L";
        }
        return "+";
    }

    private float getJoystickRadius() {
        return clamp(Math.min(viewWidth, viewHeight) * 0.13f, 48f, 88f);
    }

    private float getJoystickCenterX() {
        return getJoystickRadius() * 1.88f;
    }

    private float getJoystickCenterY() {
        return viewHeight - getJoystickRadius() * 1.68f;
    }

    private float getSpecialButtonRadius() {
        return getJoystickRadius() * 0.88f;
    }

    private float getSpecialButtonCenterX() {
        return viewWidth - getJoystickRadius() * 1.88f;
    }

    private float getSpecialButtonCenterY() {
        return getJoystickCenterY();
    }

    private boolean isSpecialButtonHit(float x, float y) {
        return distanceSquared(x, y, getSpecialButtonCenterX(), getSpecialButtonCenterY()) <= square(getSpecialButtonRadius() * 1.35f);
    }

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


    private float getTopButtonSize() {
        return isPortrait() ? clamp(viewWidth * 0.10f, 42f, 58f) : clamp(viewHeight * 0.10f, 48f, 66f);
    }

    private boolean isPauseButtonHit(float x, float y) {
        float size = getTopButtonSize();
        float left = getGameplayPauseButtonLeft();
        float top = getGameplayPauseButtonTop();
        return isInside(x, y, left, top, left + size, top + size);
    }

    private float getGameplayPauseButtonLeft() {
        return viewWidth - getTopButtonSize() - (isPortrait() ? Math.max(16f, viewWidth * 0.045f) : Math.max(24f, viewHeight * 0.052f));
    }

    private float getGameplayPauseButtonTop() {
        return isPortrait() ? Math.max(18f, viewWidth * 0.050f) : Math.max(22f, viewHeight * 0.045f);
    }

    private float getMenuButtonWidth() {
        return isPortrait() ? clamp(viewWidth * 0.24f, 110f, 170f) : clamp(viewWidth * 0.16f, 140f, 220f);
    }

    private float getMenuButtonHeight() {
        return isPortrait() ? clamp(viewWidth * 0.135f, 60f, 98f) : clamp(viewHeight * 0.13f, 58f, 90f);
    }

    private float getMenuButtonTop() {
        return isPortrait() ? viewHeight * 0.58f : viewHeight * 0.55f;
    }

    private float getMenuButtonBottom() {
        return getMenuButtonTop() + getMenuButtonHeight();
    }

    private float getMenuStartLeft() {
        float gap = Math.max(16f, viewWidth * 0.030f);
        return (viewWidth - getMenuButtonWidth() * 3f - gap * 2f) * 0.5f;
    }

    private float getMenuStartRight() {
        return getMenuStartLeft() + getMenuButtonWidth();
    }

    private float getMenuSettingsLeft() {
        return getMenuStartRight() + Math.max(16f, viewWidth * 0.030f);
    }

    private float getMenuSettingsRight() {
        return getMenuSettingsLeft() + getMenuButtonWidth();
    }

    private float getMenuShopCenterX() {
        return getMenuSettingsRight() + Math.max(16f, viewWidth * 0.030f) + getMenuButtonWidth() * 0.5f;
    }

    private boolean isMenuStartHit(float x, float y) {
        return isInside(x, y, getMenuStartLeft(), getMenuButtonTop(), getMenuStartRight(), getMenuButtonBottom());
    }

    private boolean isMenuSettingsHit(float x, float y) {
        return isInside(x, y, getMenuSettingsLeft(), getMenuButtonTop(), getMenuSettingsRight(), getMenuButtonBottom());
    }

    private float getPauseButtonWidth() {
        return Math.min(viewWidth * 0.42f, 360f);
    }

    private float getPauseButtonHeight() {
        return Math.min(viewHeight * 0.068f, 62f);
    }

    private void drawPopup(Canvas canvas) {
        paint.setColor(Color.argb(180, 0, 0, 0));
        canvas.drawRect(0f, 0f, viewWidth, viewHeight, paint);

        float pw = viewWidth * 0.72f;
        float ph = viewHeight * 0.39f;
        float px = viewWidth * 0.5f - pw * 0.5f;
        float py = viewHeight * 0.5f - ph * 0.5f;
        rect.set(px, py, px + pw, py + ph);
        paint.setColor(Color.rgb(26, 36, 46));
        canvas.drawRoundRect(rect, 20f, 20f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        paint.setColor(Color.rgb(219, 170, 72));
        canvas.drawRoundRect(rect, 20f, 20f, paint);
        paint.setStyle(Paint.Style.FILL);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(Math.max(18f, viewHeight * 0.032f));
        textPaint.setColor(Color.rgb(219, 170, 72));
        drawIronblockText(canvas, popupTitle, viewWidth * 0.5f, py + ph * 0.23f, textPaint);

        textPaint.setTextSize(Math.max(13f, viewHeight * 0.024f));
        textPaint.setColor(Color.WHITE);
        
        // Manual wrapping
        String msg = popupMessage;
        float msgY = py + ph * 0.42f;
        if (msg.length() > 30) {
            int mid = msg.lastIndexOf(' ', 30);
            if (mid > 0) {
                drawIronblockText(canvas, msg.substring(0, mid), viewWidth * 0.5f, msgY, textPaint);
                drawIronblockText(canvas, msg.substring(mid + 1), viewWidth * 0.5f, msgY + textPaint.getTextSize() * 1.25f, textPaint);
            } else {
                drawIronblockText(canvas, msg, viewWidth * 0.5f, msgY, textPaint);
            }
        } else {
            drawIronblockText(canvas, msg, viewWidth * 0.5f, msgY, textPaint);
        }

        float bw = viewWidth * 0.19f;
        float bh = viewHeight * 0.066f;
        float buttonY = viewHeight * 0.5f + ph * 0.24f;

        if (popupType == POPUP_CONFIRM || popupType == POPUP_ERROR) {
            drawButton(canvas, "YES", viewWidth * 0.5f - bw * 1.1f, buttonY, viewWidth * 0.5f - bw * 0.1f, buttonY + bh, true, false);
            drawButton(canvas, "NO", viewWidth * 0.5f + bw * 0.1f, buttonY, viewWidth * 0.5f + bw * 1.1f, buttonY + bh, true, false);
        } else {
            drawButton(canvas, "OK", viewWidth * 0.5f - bw * 0.5f, buttonY, viewWidth * 0.5f + bw * 0.5f, buttonY + bh, true, false);
        }
    }

    private void drawShop(Canvas canvas) {
        drawSettingsPanel(canvas);
        float cx = viewWidth * 0.5f;
        float cy = getSettingsPanelTop() + viewHeight * 0.12f;
        
        if (shopSprite != null) {
            drawBitmapCentered(canvas, shopSprite, cx, cy, viewHeight * 0.088f, 0f, spritePaint);
        } else {
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(Math.max(28f, viewHeight * 0.065f));
            textPaint.setColor(Color.rgb(219, 170, 72));
            drawIronblockText(canvas, "SHOP", cx, cy, textPaint);
        }

        float goldX = getSettingsPanelRight() - viewWidth * 0.088f;
        float goldY = getSettingsPanelTop() + viewHeight * 0.041f;
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTextSize(Math.max(16f, viewHeight * 0.032f));
        textPaint.setColor(Color.WHITE);
        float coinSize = Math.max(16f, viewHeight * 0.032f);
        float valueX = goldX;
        float ownedDigitH = coinSize * 0.88f;
        float ownedDigitGap = Math.max(2f, coinSize * 0.07f);
        drawSpriteNumberRightAligned(canvas, String.valueOf(gold), valueX, goldY - coinSize * 0.02f, ownedDigitH, ownedDigitGap);
        float ownedValueW = measureSpriteNumberWidth(String.valueOf(gold), ownedDigitH, ownedDigitGap);
        drawBitmapCentered(canvas, goldCoinSprite, valueX - ownedValueW - coinSize * 0.95f, goldY - coinSize * 0.08f, coinSize, 0f, spritePaint);

        // Shop Items
        float gridY = cy + viewHeight * 0.15f;
        float spacingX = viewWidth * 0.22f;
        float spacingY = viewHeight * 0.22f;
        
        drawShopItem(canvas, cx - spacingX, gridY, "100 GOLD", coinBagSprite, null);
        drawShopItem(canvas, cx + spacingX, gridY, "300 GOLD", coinChestSprite, "MOST POPULAR");
        drawShopItem(canvas, cx - spacingX, gridY + spacingY, "500 GOLD", coinChestFullSprite, null);
        drawShopItem(canvas, cx + spacingX, gridY + spacingY, "1000 GOLD", mountainCoinSprite, "BEST VALUE");

        drawSettingsBack(canvas);
    }

    private void drawShopItem(Canvas canvas, float x, float y, String label, Bitmap icon, String badge) {
        float size = Math.min(viewWidth, viewHeight) * 0.18f;
        if (badge != null && badge.length() > 0) {
            boolean isMostPopular = "MOST POPULAR".equals(badge);
            float badgeW = size * (isMostPopular ? 1.18f : 1.02f);
            float badgeH = Math.max(22f, viewHeight * 0.026f);
            float badgeY = y - size * 0.66f;
            rect.set(x - badgeW * 0.5f, badgeY - badgeH * 0.5f, x + badgeW * 0.5f, badgeY + badgeH * 0.5f);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(208, 34, 42, 52));
            canvas.drawRoundRect(rect, 8f, 8f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f);
            paint.setColor(Color.argb(220, 245, 211, 118));
            canvas.drawRoundRect(rect, 8f, 8f, paint);
            paint.setStyle(Paint.Style.FILL);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTextSize(Math.max(12f, viewHeight * 0.015f));
            textPaint.setColor(Color.rgb(255, 229, 166));
            drawIronblockText(canvas, badge, x, badgeY + badgeH * 0.12f, textPaint);
        }
        if (icon != null) {
            drawBitmapCentered(canvas, icon, x, y, size, 0f, spritePaint);
        } else {
            paint.setColor(Color.argb(120, 255, 255, 255));
            canvas.drawCircle(x, y, size * 0.5f, paint);
        }
        
        String value = label.replaceAll("[^0-9]", "");
        float labelY = y + size * 0.72f;
        float coinS = Math.max(16f, viewHeight * 0.024f);
        float digitH = coinS * 1.08f;
        float digitGap = Math.max(2f, coinS * 0.06f);
        float digitsW = measureSpriteNumberWidth(value, digitH, digitGap);
        float clusterW = coinS + coinS * 0.58f + digitsW;
        float clusterLeft = x - clusterW * 0.5f;
        drawBitmapCentered(canvas, goldCoinSprite, clusterLeft + coinS * 0.5f, labelY - coinS * 0.08f, coinS, 0f, spritePaint);
        float numberCenterX = clusterLeft + coinS + coinS * 0.58f + digitsW * 0.5f;
        drawSpriteNumberCentered(canvas, value, numberCenterX, labelY, digitH, digitGap);
    }

    private void handleShopTouch(float x, float y) {
        if (isSettingsBackHit(x, y)) {
            audio.playButton();
            gameScreen = SCREEN_SETTINGS;
            return;
        }
        
        float cx = viewWidth * 0.5f;
        float cy = getSettingsPanelTop() + viewHeight * 0.12f;
        float gridY = cy + viewHeight * 0.15f;
        float spacingX = viewWidth * 0.22f;
        float spacingY = viewHeight * 0.22f;
        float hitR = viewWidth * 0.12f;

        if (distanceSquared(x, y, cx - spacingX, gridY) < square(hitR)) promptBuyGold(PRODUCT_GOLD_100, 100);
        if (distanceSquared(x, y, cx + spacingX, gridY) < square(hitR)) promptBuyGold(PRODUCT_GOLD_300, 300);
        if (distanceSquared(x, y, cx - spacingX, gridY + spacingY) < square(hitR)) promptBuyGold(PRODUCT_GOLD_500, 500);
        if (distanceSquared(x, y, cx + spacingX, gridY + spacingY) < square(hitR)) promptBuyGold(PRODUCT_GOLD_1000, 1000);
    }

    private void promptBuyGold(final String productId, final int amount) {
        if (!(getContext() instanceof Activity)) {
            return;
        }
        final Activity activity = (Activity) getContext();
        audio.playButton();
        purchaseManager.launchBuyGold(activity, productId, amount, new PurchaseManager.Callback() {
            @Override
            public void onGoldGranted(int purchasedAmount, String message) {
                activity.runOnUiThread(() -> {
                    gold += purchasedAmount;
                    saveSettings();
                    new AlertDialog.Builder(activity)
                            .setTitle("Purchase Complete")
                            .setMessage(message)
                            .setPositiveButton("OK", null)
                            .show();
                });
            }

            @Override
            public void onPurchasePending(String message) {
                showStoreMessage(activity, "Purchase Pending", message);
            }

            @Override
            public void onPurchaseFailed(String message) {
                showStoreMessage(activity, "Purchase Failed", message);
            }

            @Override
            public void onStoreUnavailable(String message) {
                showStoreMessage(activity, "Store Unavailable", message);
            }
        });
    }

    private void showStoreMessage(Activity activity, String title, String message) {
        activity.runOnUiThread(() -> new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show());
    }

    private String getPlaneTraitName(int planeIndex) {
        switch (planeIndex) {
            case 2:
                return "JET BURST";
            case 3:
                return "ARMORED VOLLEY";
            case 4:
                return "VAMPIRE DASH";
            case 5:
                return "HEAVY STRIKER";
            case 6:
                return "FORTRESS JET";
            case 1:
                return "BALANCED ACE";
            default:
                return "CLASSIC FIGHTER";
        }
    }

    private String getPlaneTraitDetail(int planeIndex) {
        switch (planeIndex) {
            case 2:
                return "+Damage, slower fire cadence";
            case 3:
                return "High armor with faster fire";
            case 4:
                return "Very fast and rapid-fire style";
            case 5:
                return "Highest strike damage profile";
            case 6:
                return "Tanky frame with fast fire";
            case 1:
                return "Stable all-round performance";
            default:
                return "Balanced starter aircraft";
        }
    }

    private float getPauseButtonTop(int index) {
        float h = getPauseButtonHeight();
        float gap = Math.max(12f, viewHeight * 0.012f);
        float startY = viewHeight * 0.47f;
        return startY + index * (h + gap);
    }

    private float getPauseButtonLeft() {
        return (viewWidth - getPauseButtonWidth()) * 0.5f;
    }

    private boolean isPauseResumeHit(float x, float y) {
        return isInside(x, y, getPauseButtonLeft(), getPauseButtonTop(0), getPauseButtonLeft() + getPauseButtonWidth(), getPauseButtonTop(0) + getPauseButtonHeight());
    }

    private boolean isPauseSettingsHit(float x, float y) {
        return isInside(x, y, getPauseButtonLeft(), getPauseButtonTop(1), getPauseButtonLeft() + getPauseButtonWidth(), getPauseButtonTop(1) + getPauseButtonHeight());
    }

    private boolean isPauseRestartHit(float x, float y) {
        return isInside(x, y, getPauseButtonLeft(), getPauseButtonTop(2), getPauseButtonLeft() + getPauseButtonWidth(), getPauseButtonTop(2) + getPauseButtonHeight());
    }

    private boolean isPauseMenuHit(float x, float y) {
        return isInside(x, y, getPauseButtonLeft(), getPauseButtonTop(3), getPauseButtonLeft() + getPauseButtonWidth(), getPauseButtonTop(3) + getPauseButtonHeight());
    }

    private float getSettingsPanelLeft() {
        return isPortrait() ? viewWidth * 0.055f : viewWidth * 0.07f;
    }

    private float getSettingsPanelTop() {
        return isPortrait() ? viewHeight * 0.055f : viewHeight * 0.07f;
    }

    private float getSettingsPanelRight() {
        return isPortrait() ? viewWidth * 0.945f : viewWidth * 0.93f;
    }

    private float getSettingsPanelBottom() {
        return isPortrait() ? viewHeight * 0.955f : viewHeight * 0.93f;
    }

    private float getSettingsPreviewCenterX() {
        return isPortrait() ? viewWidth * 0.50f : viewWidth * 0.28f;
    }

    private float getSettingsPreviewCenterY() {
        return isPortrait() ? viewHeight * 0.405f : viewHeight * 0.52f;
    }

    private float getSettingsPreviewHalfWidth() {
        return isPortrait() ? viewWidth * 0.265f : viewWidth * 0.18f;
    }

    private float getSettingsPreviewHalfHeight() {
        return isPortrait() ? viewHeight * 0.120f : viewHeight * 0.23f;
    }

    private float getSettingsLabelTextSize() {
        return isPortrait() ? clamp(viewWidth * 0.055f, 20f, 28f) : Math.max(18f, viewHeight * 0.045f);
    }

    private float getSettingsBackLeft() {
        return isPortrait() ? getSettingsPanelLeft() + viewWidth * 0.028f : viewWidth * 0.095f;
    }

    private float getSettingsBackTop() {
        return isPortrait() ? getSettingsPanelTop() + viewHeight * 0.028f : viewHeight * 0.100f;
    }

    private float getSettingsBackRight() {
        return getSettingsBackLeft() + (isPortrait() ? clamp(viewWidth * 0.18f, 80f, 110f) : clamp(viewWidth * 0.16f, 100f, 150f));
    }

    private float getSettingsBackBottom() {
        return getSettingsBackTop() + getSmallButtonHeight();
    }

    private boolean isSettingsBackHit(float x, float y) {
        return isInside(x, y, getSettingsBackLeft(), getSettingsBackTop(), getSettingsBackRight(), getSettingsBackBottom());
    }

    private float getSmallButtonHeight() {
        return isPortrait() ? clamp(viewWidth * 0.11f, 48f, 64f) : clamp(viewHeight * 0.10f, 52f, 68f);
    }

    private float getSelectorPrevLeft() {
        return getSettingsPanelLeft() + (isPortrait() ? viewWidth * 0.035f : viewWidth * 0.045f);
    }

    private float getSelectorPrevRight() {
        return getSelectorPrevLeft() + getSmallButtonHeight() * 1.25f;
    }

    private float getSelectorNameLeft() {
        return getSelectorPrevRight() + (isPortrait() ? viewWidth * 0.018f : viewWidth * 0.012f);
    }

    private float getSelectorNameRight() {
        return isPortrait() ? getSelectorNextLeft() - viewWidth * 0.018f : viewWidth * 0.81f;
    }

    private float getSelectorNextLeft() {
        if (isPortrait()) {
            return getSettingsPanelRight() - viewWidth * 0.035f - getSmallButtonHeight() * 1.25f;
        }
        return getSelectorNameRight() + viewWidth * 0.025f;
    }

    private float getSelectorNextRight() {
        return getSelectorNextLeft() + getSmallButtonHeight() * 1.25f;
    }

    private boolean isPlanePrevHit(float x, float y) {
        float top = getSelectorTop();
        return isInside(x, y, getSelectorPrevLeft(), top, getSelectorPrevRight(), top + getSmallButtonHeight());
    }

    private boolean isPlaneNextHit(float x, float y) {
        float top = getSelectorTop();
        return isInside(x, y, getSelectorNextLeft(), top, getSelectorNextRight(), top + getSmallButtonHeight());
    }

    private float getSelectorTop() {
        return isPortrait() ? viewHeight * 0.215f : viewHeight * 0.24f;
    }

    private float getControlStyleLeft() {
        return isPortrait() ? getSettingsPanelLeft() + viewWidth * 0.060f : viewWidth * 0.50f;
    }

    private float getControlStyleTop() {
        return isPortrait() ? viewHeight * 0.575f : viewHeight * 0.405f;
    }

    private float getControlStyleBottom() {
        return getControlStyleTop() + getSmallButtonHeight();
    }

    private float getControlFixedLeft() {
        return getControlStyleLeft();
    }

    private float getControlFixedRight() {
        return isPortrait()
                ? getControlFixedLeft() + (getSettingsPanelRight() - getSettingsPanelLeft() - viewWidth * 0.15f) * 0.5f
                : getControlFixedLeft() + clamp(viewWidth * 0.155f, 120f, 174f);
    }

    private float getControlDragLeft() {
        return getControlFixedRight() + viewWidth * 0.018f;
    }

    private float getControlDragRight() {
        return isPortrait()
                ? getSettingsPanelRight() - viewWidth * 0.060f
                : getControlDragLeft() + clamp(viewWidth * 0.135f, 112f, 162f);
    }

    private boolean isControlFixedHit(float x, float y) {
        return isInside(x, y, getControlFixedLeft(), getControlStyleTop(), getControlFixedRight(), getControlStyleBottom());
    }

    private boolean isControlDragHit(float x, float y) {
        return isInside(x, y, getControlDragLeft(), getControlStyleTop(), getControlDragRight(), getControlStyleBottom());
    }

    private float getGraphicsStyleLeft() {
        return isPortrait() ? getSettingsPanelLeft() + viewWidth * 0.060f : viewWidth * 0.50f;
    }

    private float getGraphicsStyleTop() {
        return isPortrait() ? viewHeight * 0.708f : viewHeight * 0.555f;
    }

    private float getGraphicsStyleBottom() {
        return getGraphicsStyleTop() + getSmallButtonHeight();
    }

    private float getPrivacyButtonLeft() {
        return getSliderLeft();
    }

    private float getPrivacyButtonRight() {
        return getSliderRight();
    }

    private float getPrivacyButtonTop() {
        return isPortrait() ? viewHeight * 0.765f : viewHeight * 0.628f;
    }

    private float getPrivacyButtonBottom() {
        return getPrivacyButtonTop() + getSmallButtonHeight();
    }

    private boolean isPrivacyPolicyHit(float x, float y) {
        return isInside(x, y, getPrivacyButtonLeft(), getPrivacyButtonTop(), getPrivacyButtonRight(), getPrivacyButtonBottom());
    }

    private float getGraphicsFullLeft() {
        return getGraphicsStyleLeft();
    }

    private boolean isPlaneNameHit(float x, float y) {
        float top = getSelectorTop();
        return isInside(x, y, getSelectorNameLeft(), top, getSelectorNameRight(), top + getSmallButtonHeight());
    }

    private boolean isReviveButtonHit(float x, float y) {
        float btnW = Math.min(viewWidth * 0.65f, 520f);
        float btnH = btnW * 0.26f;
        float btnX = viewWidth * 0.50f - btnW * 0.50f;
        float btnY = getReviveButtonTopY();
        return isInside(x, y, btnX, btnY, btnX + btnW, btnY + btnH);
    }

    private float getReviveButtonTopY() {
        return viewHeight * 0.72f;
    }

    private float getEndMenuButtonHeight() {
        return viewHeight * 0.050f;
    }

    private float getEndMenuButtonWidth() {
        return viewWidth * 0.32f;
    }

    private float getEndRestartButtonCenterY() {
        float bh = getEndMenuButtonHeight();
        float reviveW = Math.min(viewWidth * 0.65f, 520f);
        float reviveH = reviveW * 0.26f;
        float interGap = viewHeight * 0.030f;
        return getReviveButtonTopY() + reviveH + interGap + bh * 0.5f;
    }

    private float getEndMainMenuButtonCenterY() {
        float bh = getEndMenuButtonHeight();
        float interGap = viewHeight * 0.030f;
        return getEndRestartButtonCenterY() + bh + interGap;
    }

    private void revivePlayer() {
        hasRevived = true;
        gameOver = false;
        lives = 1;
        playerHp = getPlayerMaxHp();
        invulnerableTimer = 3.0f;
        audio.pauseMusic();
        
        // Spawn all power-ups
        dropPickup(playerX - 60, playerY, POWER_GUN);
        dropPickup(playerX - 20, playerY - 40, POWER_BARRAGE);
        dropPickup(playerX + 20, playerY - 40, POWER_ROCKET);
        dropPickup(playerX + 60, playerY, POWER_SPECIAL);
        dropPickup(playerX, playerY - 80, POWER_ARMOR);
        dropPickup(playerX, playerY + 40, POWER_GUARDIAN);
        
        if (getContext() instanceof MainActivity) {
            ((MainActivity) getContext()).showInterstitialAd();
        }
    }

    private float getGraphicsFullRight() {
        return isPortrait()
                ? getGraphicsFullLeft() + (getSettingsPanelRight() - getSettingsPanelLeft() - viewWidth * 0.15f) * 0.5f
                : getGraphicsFullLeft() + clamp(viewWidth * 0.105f, 86f, 118f);
    }

    private float getGraphicsSaverLeft() {
        return getGraphicsFullRight() + viewWidth * 0.018f;
    }

    private float getGraphicsSaverRight() {
        return isPortrait()
                ? getSettingsPanelRight() - viewWidth * 0.060f
                : getGraphicsSaverLeft() + clamp(viewWidth * 0.185f, 138f, 210f);
    }

    private boolean isGraphicsFullHit(float x, float y) {
        return isInside(x, y, getGraphicsFullLeft(), getGraphicsStyleTop(), getGraphicsFullRight(), getGraphicsStyleBottom());
    }

    private boolean isGraphicsSaverHit(float x, float y) {
        return isInside(x, y, getGraphicsSaverLeft(), getGraphicsStyleTop(), getGraphicsSaverRight(), getGraphicsStyleBottom());
    }

    private float getSliderLeft() {
        return isPortrait() ? getSettingsPanelLeft() + viewWidth * 0.065f : viewWidth * 0.50f;
    }

    private float getSliderRight() {
        return isPortrait() ? getSettingsPanelRight() - viewWidth * 0.065f : viewWidth * 0.84f;
    }

    private float getMusicSliderY() {
        return isPortrait() ? viewHeight * 0.818f : viewHeight * 0.700f;
    }

    private float getSfxSliderY() {
        return isPortrait() ? viewHeight * 0.902f : viewHeight * 0.826f;
    }

    private int hitSlider(float x, float y) {
        float touch = Math.max(24f, viewHeight * 0.045f);
        if (isInside(x, y, getSliderLeft() - touch, getMusicSliderY() - touch, getSliderRight() + touch, getMusicSliderY() + touch)) {
            return SLIDER_MUSIC;
        }
        if (isInside(x, y, getSliderLeft() - touch, getSfxSliderY() - touch, getSliderRight() + touch, getSfxSliderY() + touch)) {
            return SLIDER_SFX;
        }
        return SLIDER_NONE;
    }

    private boolean isInside(float x, float y, float left, float top, float right, float bottom) {
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    private boolean isPortrait() {
        return viewHeight >= viewWidth;
    }

    private int lighten(int color) {
        return Color.rgb(
                Math.min(255, (int) (Color.red(color) * 1.45f + 22f)),
                Math.min(255, (int) (Color.green(color) * 1.45f + 22f)),
                Math.min(255, (int) (Color.blue(color) * 1.45f + 22f)));
    }

    private int darken(int color) {
        return Color.rgb(
                Math.max(0, (int) (Color.red(color) * 0.55f)),
                Math.max(0, (int) (Color.green(color) * 0.55f)),
                Math.max(0, (int) (Color.blue(color) * 0.55f)));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float lerp(float a, float b, float amount) {
        return a + (b - a) * amount;
    }

    private static float square(float value) {
        return value * value;
    }

    private static float distanceSquared(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return dx * dx + dy * dy;
    }

    private static void sleepForNanos(long nanos) {
        if (nanos <= 0L) {
            Thread.yield();
            return;
        }
        if (nanos < 500_000L) {
            Thread.yield();
            return;
        }
        long millis = nanos / 1_000_000L;
        int extraNanos = (int) (nanos - millis * 1_000_000L);
        try {
            Thread.sleep(millis, extraNanos);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class Bullet {
        float x;
        float y;
        float vx;
        float vy;
        float damage;
        float radius;
        float life;
        boolean rocket;
    }

    private static final class EnemyShot {
        float x;
        float y;
        float vx;
        float vy;
        float life;
    }

    private static final class Enemy {
        float x;
        float y;
        float vx;
        float vy;
        float size;
        float hp;
        float maxHp;
        float fireTimer;
        float hitTimer;
        float phase;
        float spiralAngle;
        float spiralShotTimer;
        float laserWarmup;
        float laserTimer;
        float laserHitTimer;
        float laserX;
        float laserWidth;
        int spiralShotsLeft;
        int bossPattern;
        int spriteIndex;
        int type;
        boolean dead;
    }

    private static final class Pickup {
        float x;
        float y;
        float vy;
        float spin;
        float phase;
        int type;
        int goldAmount;
    }

    private static final class PickupText {
        float x;
        float y;
        float vy;
        float age;
        float life;
        String text;
    }

    private static final class Explosion {
        float x;
        float y;
        float vx;
        float vy;
        float radius;
        float age;
        float life;
        int color;
        boolean smoke;
    }

    private static final class Confetti {
        float x;
        float y;
        float vx;
        float vy;
        float size;
        float rotation;
        float spin;
        float age;
        float life;
        int color;
    }

    private static final class Cloud {
        float x;
        float y;
        float size;
        float speed;
        float direction;
        float driftTimer;
        float pauseTimer;
        boolean smoke;
    }
}
