import { getComponent } from '/components/Loader.js'

const modules = {
    all: [],
    automation: {},
    esp: {},
    hacks: {},
    visuals: {},
    scripting: {},
    utility: {}
};

const module = (params) => {
    params.componentRef = getComponent(`${params.group}/${params.component}`);

    modules.all.push(params);
    modules[params.group][params.component] = params;
};

module({
    group: 'automation',
    name: 'Auto Disconnect',
    component: 'AutoDisconnect',
    path: 'auto-disconnect',
    tags: ['auto', 'disconnect']
});
module({
    group: 'automation',
    name: 'Auto Bucket',
    component: 'AutoBucket',
    path: 'auto-bucket',
    tags: ['auto', 'bucket', 'mlg']
});
module({
    group: 'automation',
    name: 'Auto Fish',
    component: 'AutoFish',
    path: 'auto-fish',
    tags: ['auto', 'fish', 'fishing']
});
module({
    group: 'automation',
    name: 'Auto Totem',
    component: 'AutoTotem',
    path: 'auto-totem',
    tags: ['auto', 'totem']
});
module({
    group: 'automation',
    name: 'Auto Drop',
    component: 'AutoDrop',
    path: 'auto-drop',
    tags: ['auto', 'drop', 'inventory']
});
module({
    group: 'automation',
    name: 'Container Buttons',
    component: 'ContainerButtons',
    path: 'container-buttons',
    tags: ['container', 'buttons']
});
module({
    group: 'automation',
    name: 'Auto Eat',
    component: 'AutoEat',
    path: 'auto-eat',
    tags: ['auto', 'eat']
});
module({
    group: 'automation',
    name: 'Anti Respawn Reset',
    component: 'AntiRespawnReset',
    path: 'anti-respawn-reset',
    tags: ['anti', 'respawn', 'bed', 'anchor']
});
module({
    group: 'automation',
    name: 'Auto Craft',
    component: 'AutoCraft',
    path: 'auto-craft',
    tags: ['auto', 'craft']
});
module({
    group: 'automation',
    name: 'Auto Attack',
    component: 'AutoAttack',
    path: 'auto-attack',
    tags: ['auto', 'attack']
});
module({
    group: 'automation',
    name: 'Schematica (ALPHA)',
    component: 'Schematica',
    path: 'schematica',
    tags: ['schematica']
});
module({
    group: 'automation',
    name: 'Auto Hotbar',
    component: 'AutoHotbar',
    path: 'auto-hotbar',
    tags: ['auto', 'hotbar']
});

module({
    group: 'esp',
    name: 'Block ESP',
    component: 'BlockESP',
    path: 'block-esp',
    tags: ['blocks', 'esp', 'xray']
});
module({
    group: 'esp',
    name: 'Entity ESP',
    component: 'EntityESP',
    path: 'entity-esp',
    tags: ['entity', 'entities', 'esp']
});
module({
    group: 'esp',
    name: 'Projectile Path',
    component: 'ProjectilePath',
    path: 'projectile-path',
    tags: ['projectile', 'path', 'ender', 'pearl']
});
module({
    group: 'esp',
    name: 'Light Level',
    component: 'LightLevel',
    path: 'light-level',
    tags: ['light', 'level', 'mob', 'spawn']
});
module({
    group: 'esp',
    name: 'End City Chunks',
    component: 'EndCityChunks',
    path: 'end-city-chunks',
    tags: ['end', 'city', 'cities', 'chunks']
});
module({
    group: 'esp',
    name: 'Entity Owner',
    component: 'EntityOwner',
    path: 'entity-owner',
    tags: ['entity', 'owner']
});
module({
    group: 'esp',
    name: 'Free Cam',
    component: 'FreeCam',
    path: 'freecam',
    tags: ['freecam', 'camera']
});
module({
    group: 'esp',
    name: 'New Chunks',
    component: 'NewChunks',
    path: 'new-chunks',
    tags: ['new', 'chunks']
});
module({
    group: 'esp',
    name: 'Entity Titles',
    component: 'EntityTitle',
    path: 'entity-titles',
    tags: ['entity', 'title', 'health']
});

module({
    group: 'hacks',
    name: 'Kill Aura',
    component: 'KillAura',
    path: 'kill-aura',
    tags: ['kill', 'aura', 'auto', 'attack']
});
module({
    group: 'hacks',
    name: 'Elytra Fly',
    component: 'ElytraHack',
    path: 'elytra-fly',
    tags: ['elytra', 'hack', 'fly']
});
module({
    group: 'hacks',
    name: 'Pig',
    component: 'PigHack',
    path: 'pig',
    tags: ['pig', 'hack']
});
module({
    group: 'hacks',
    name: 'Auto Criticals',
    component: 'AutoCriticals',
    path: 'auto-criticals',
    tags: ['auto', 'criticals']
});
module({
    group: 'hacks',
    name: 'Fly',
    component: 'FlyHack',
    path: 'fly',
    tags: ['fly', 'hack']
});
module({
    group: 'hacks',
    name: 'Elytra Tunnel',
    component: 'ElytraTunnel',
    path: 'elytra-tunnel',
    tags: ['elytra', 'tunnel']
});
module({
    group: 'hacks',
    name: 'Movement',
    component: 'MovementHack',
    path: 'movement',
    tags: ['movement', 'hack']
});
module({
    group: 'hacks',
    name: 'Scaffold',
    component: 'Scaffold',
    path: 'scaffold',
    tags: ['scaffold']
});
module({
    group: 'hacks',
    name: 'No Fall',
    component: 'NoFall',
    path: 'no-fall',
    tags: ['nofall', 'no', 'fall']
});
module({
    group: 'hacks',
    name: 'Fast Break',
    component: 'FastBreak',
    path: 'fast-break',
    tags: ['fast', 'break']
});
module({
    group: 'hacks',
    name: 'Reach',
    component: 'Reach',
    path: 'reach',
    tags: ['reach']
});
module({
    group: 'hacks',
    name: 'Teleport',
    component: 'TeleportHack',
    path: 'teleport',
    tags: ['teleport', 'hack']
});
module({
    group: 'hacks',
    name: 'Fake Lag',
    component: 'FakeLag',
    path: 'fake-lag',
    tags: ['fake', 'lag']
});
module({
    group: 'hacks',
    name: 'Blink',
    component: 'Blink',
    path: 'blink',
    tags: ['blink']
});
module({
    group: 'hacks',
    name: 'Boat',
    component: 'BoatHack',
    path: 'boat',
    tags: ['boat', 'hack', 'fly']
});
module({
    group: 'hacks',
    name: 'Inv Move',
    component: 'InvMove',
    path: 'inv-move',
    tags: ['inventory', 'move', 'keys']
});
module({
    group: 'hacks',
    name: 'Area Mine',
    component: 'AreaMine',
    path: 'area-mine',
    tags: ['area', 'mine']
});
module({
    group: 'hacks',
    name: 'Server Plugins',
    component: 'ServerPlugins',
    path: 'server-plugins',
    tags: ['server', 'plugins']
});
module({
    group: 'hacks',
    name: 'Hitbox Size',
    component: 'HitboxSize',
    path: 'hitbox-size',
    tags: ['hitbox', 'size']
});
module({
    group: 'hacks',
    name: 'Bedrock Breaker',
    component: 'BedrockBreaker',
    path: 'bedrock-breaker',
    tags: ['bedrock', 'breaker']
});
module({
    group: 'hacks',
    name: 'Anti Hunger',
    component: 'AntiHunger',
    path: 'anti-hunger',
    tags: ['anti', 'hunger']
});
module({
    group: 'hacks',
    name: 'Step Up',
    component: 'StepUp',
    path: 'step-up',
    tags: ['step', 'up', 'jump']
});

module({
    group: 'visuals',
    name: 'Full Bright',
    component: 'FullBright',
    path: 'full-bright',
    tags: ['full', 'bright', 'night', 'vision']
});
module({
    group: 'visuals',
    name: 'Armor Overlay',
    component: 'ArmorOverlay',
    path: 'armor-overlay',
    tags: ['armor', 'overlay']
});
module({
    group: 'visuals',
    name: 'Shulker Tooltip',
    component: 'ShulkerTooltip',
    path: 'shulker-tooltip',
    tags: ['shulker', 'tooltip']
});
module({
    group: 'visuals',
    name: 'Advanced Tooltips',
    component: 'AdvancedTooltips',
    path: 'adv-tooltips',
    tags: ['advanced', 'tooltips']
});
module({
    group: 'visuals',
    name: 'Exploration Mini Map',
    component: 'ExplorationMiniMap',
    path: 'exploration-mini-map',
    tags: ['exploration', 'minimap']
});
module({
    group: 'visuals',
    name: 'Death Coordinates',
    component: 'DeathCoordinates',
    path: 'death-coordinates',
    tags: ['death', 'coordinates']
});
module({
    group: 'visuals',
    name: 'No Fog',
    component: 'Fog',
    path: 'no-fog',
    tags: ['fog']
});
module({
    group: 'visuals',
    name: 'Chunks',
    component: 'Chunks',
    path: 'chunks',
    tags: ['chunks', 'distance']
});
module({
    group: 'visuals',
    name: 'Status Effects',
    component: 'StatusEffects',
    path: 'status-effects',
    tags: ['status', 'effects']
});
module({
    group: 'visuals',
    name: 'Particles',
    component: 'Particles',
    path: 'particles',
    tags: ['particles']
});
module({
    group: 'visuals',
    name: 'Zoom',
    component: 'Zoom',
    path: 'zoom',
    tags: ['zoom']
});
module({
    group: 'visuals',
    name: 'Performance',
    component: 'Performance',
    path: 'performance',
    tags: ['performance', 'fps']
});
module({
    group: 'visuals',
    name: 'World Markers',
    component: 'WorldMarkers',
    path: 'world-markers',
    tags: ['world', 'markers']
});
module({
    group: 'visuals',
    name: 'Hurt Bobbing',
    component: 'BobHurt',
    path: 'bob-hurt',
    tags: ['nohurtcam', 'bobhurt']
});
module({
    group: 'visuals',
    name: 'No Weather',
    component: 'NoWeather',
    path: 'no-weather',
    tags: ['no', 'weather']
});
module({
    group: 'visuals',
    name: 'Fake Weather',
    component: 'FakeWeather',
    path: 'fake-weather',
    tags: ['fake', 'weather']
});

module({
    group: 'scripting',
    name: 'Key Bindings',
    component: 'KeyBindingScripts',
    path: 'key-bindings',
    tags: ['key', 'bindings', 'scripting']
});
module({
    group: 'scripting',
    name: 'Status Overlay',
    component: 'StatusOverlay',
    path: 'status-overlay',
    tags: ['status', 'overlay', 'f3']
});
module({
    group: 'scripting',
    name: 'Events Scripting',
    component: 'EventsScripting',
    path: 'events-scripting',
    tags: ['events', 'tick', 'scripting']
});
module({
    group: 'scripting',
    name: 'Block Automation',
    component: 'BlockAutomation',
    path: 'block-automation',
    tags: ['scripted', 'block', 'placer', 'automation']
});
module({
    group: 'scripting',
    name: 'Exec',
    component: 'Exec',
    path: 'exec',
    tags: ['exec']
});
module({
    group: 'scripting',
    name: 'Villager Roller',
    component: 'VillagerRoller',
    path: 'villager-roller',
    tags: ['villager', 'roller']
});
module({
    group: 'scripting',
    name: 'TPS',
    component: 'Tps',
    path: 'tps',
    tags: ['tps', 'tick', 'rate']
});
module({
    group: 'scripting',
    name: 'Debug',
    component: 'Debugging',
    path: 'debugging',
    tags: ['script', 'debug']
});
module({
    group: 'scripting',
    name: 'Editor Config',
    component: 'MonacoEditorConfig',
    path: 'editor-config',
    tags: ['script', 'editor', 'config', 'monaco']
});

module({
    group: 'utility',
    name: 'Core Config',
    component: 'Core',
    path: 'core',
    tags: ['core', 'port']
});
module({
    group: 'utility',
    name: 'Profiles',
    component: 'Profiles',
    path: 'profiles',
    tags: ['profiles']
});
module({
    group: 'utility',
    name: 'User Name',
    component: 'UserName',
    path: 'user-name',
    tags: ['user', 'name']
});
module({
    group: 'utility',
    name: 'Container Summary',
    component: 'ContainerSummary',
    path: 'container-summary',
    tags: ['container', 'summary']
});
module({
    group: 'utility',
    name: 'Chat',
    component: 'ChatUtilities',
    path: 'chat',
    tags: ['chat']
});
module({
    group: 'utility',
    name: 'Lock Inputs',
    component: 'LockInputs',
    path: 'lock-inputs',
    tags: ['lock', 'inputs']
});
module({
    group: 'utility',
    name: 'World Download',
    component: 'WorldDownload',
    path: 'world-download',
    tags: ['world', 'download']
});

export { modules }