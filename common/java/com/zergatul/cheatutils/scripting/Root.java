package com.zergatul.cheatutils.scripting;

import com.zergatul.cheatutils.scripting.modules.*;

@SuppressWarnings("unused")
public class Root {

    public static EspApi esp = new EspApi();

    // automation
    public static AutoDropApi autoDrop = new AutoDropApi();
    public static AutoEatApi autoEat = new AutoEatApi();
    public static ContainerButtonsApi containerButtons = new ContainerButtonsApi();
    public static AutoCraftApi autoCraft = new AutoCraftApi();
    public static AutoBucketApi autoBucket = new AutoBucketApi();
    public static AutoHotbarApi autoHotbar = new AutoHotbarApi();

    // ESP
    public static BlocksApi blocks = new BlocksApi();
    public static EntitiesApi entities = new EntitiesApi();
    public static FreeCamApi freeCam = new FreeCamApi();
    public static LightLevelApi lightLevel = new LightLevelApi();

    // hacks
    public static AutoCriticalsApi autoCriticals = new AutoCriticalsApi();
    public static BlinkApi blink = new BlinkApi();
    public static BoatHackApi boatHack = new BoatHackApi();
    public static FakeLagApi fakeLag = new FakeLagApi();
    public static FastBreakApi fastBreak = new FastBreakApi();
    public static FlyHackApi flyHack = new FlyHackApi();
    public static KillAuraApi killAura = new KillAuraApi();
    public static MovementApi movement = new MovementApi();
    public static NoFallApi noFall = new NoFallApi();
    public static ScaffoldApi scaffold = new ScaffoldApi();
    public static TeleportApi teleport = new TeleportApi();
    public static ElytraHackApi elytraFly = new ElytraHackApi();
    public static ServerPluginsApi serverPlugins = new ServerPluginsApi();
    public static HitboxSizeApi hitboxSize = new HitboxSizeApi();
    public static BedrockBreakerApi bedrockBreaker = new BedrockBreakerApi();
    public static AreaMineApi areaMine = new AreaMineApi();

    // visuals
    public static ArmorOverlayApi armorOverlay = new ArmorOverlayApi();
    public static ExplorationMiniMapApi explorationMiniMap = new ExplorationMiniMapApi();
    public static ShulkerTooltipApi shulkerTooltip = new ShulkerTooltipApi();
    public static ZoomApi zoom = new ZoomApi();
    public static FullBrightApi fullBright = new FullBrightApi();

    // scripting
    public static OverlayApi overlay = new OverlayApi();
    public static BlockAutomationApi blockAutomation = new BlockAutomationApi();
    public static VillagerRollerApi villagerRoller = new VillagerRollerApi();
    public static EventsApi events = new EventsApi();

    // utility
    public static LockInputsApi lockInputs = new LockInputsApi();
    public static TpsApi tps = new TpsApi();
    public static ProfilesApi profiles = new ProfilesApi();

    // others
    public static GameApi game = new GameApi();
    public static InputApi input = new InputApi();
    public static KeysApi keys = new KeysApi();
    public static PlayerApi player = new PlayerApi();
    public static InventoryApi inventory = new InventoryApi();
    public static VariablesApi variables = new VariablesApi();
    public static MathApi math = new MathApi();
    public static ColorApi color = new ColorApi();
    public static TimeApi time = new TimeApi();
    public static PacketApi packet = new PacketApi();
    public static SoundApi sound = new SoundApi();
    public static WindowApi window = new WindowApi();
    public static FontApi font = new FontApi();
    public static DelayApi delay = new DelayApi();
    public static DelayedApi delayed = new DelayedApi();
    public static CameraApi camera = new CameraApi();
    public static ClipboardApi clipboard = new ClipboardApi();
    public static ContainersApi containers = new ContainersApi();
    public static UIApi ui = new UIApi();
    public static HttpApi http = new HttpApi();
    public static DebugApi debug = new DebugApi();
}