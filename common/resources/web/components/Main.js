import * as FallbackLoader from '/fallback-loader.js'
import { modules } from '/modules.js'
import * as http from '/http.js'
import * as events from '/events-service.js'
import { getComponent } from '/components/Loader.js'

const { ref, onMounted, onUnmounted } = await FallbackLoader.vue();

const button = getComponent('ModuleButton');

export function createComponent(template) {
    return {
        components: {
            ModuleButton: button
        },
        template,
        setup() {
            const automation = ref(modules.automation);
            const esp = ref(modules.esp);
            const hacks = ref(modules.hacks);
            const visuals = ref(modules.visuals);
            const scripting = ref(modules.scripting);
            const utility = ref(modules.utility);
            const statuses = ref({});
            const filtered = ref({});

            const onFilter = value => {
                const map = {};

                let words = value.split(/\s+/).filter(w => w).map(w => w.toLowerCase());
                if (words.length == 0) {
                    for (let module of modules.all) {
                        map[module.component] = true;
                    }
                } else {
                    for (let module of modules.all) {
                        const tags = module.tags;
                        if (words.every(w => tags.some(t => t.indexOf(w) >= 0))) {
                            map[module.component] = true;
                        }
                    }
                }

                filtered.value = map;
            };

            const onEvent = event => {
                if (event.type == 'filter') {
                    onFilter(event.value);
                }
            };

            const onModuleClick = module => {
                window.location.hash = '#/' + module.path;
            };

            const onMatrixServerClick = () => {
                window.open('https://matrix.to/#/#cheatutils:matrix.org', '_blank');
            };

            onMounted(() => {
                onFilter('');
                events.subscribe(onEvent);

                http.get('/api/modules-status').then(response => {
                    statuses.value = response;
                });
                events.trigger({
                    type: 'focus-filter'
                });
            });
            onUnmounted(() => {
                events.unsubscribe(onEvent);
            });

            return {
                automation,
                esp,
                hacks,
                visuals,
                scripting,
                utility,
                statuses,
                filtered,

                onModuleClick,
                onMatrixServerClick
            };
        }
    };
}