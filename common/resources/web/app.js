import * as FallbackLoader from '/fallback-loader.js'
import * as http from '/http.js'
import { getComponent } from '/components/Loader.js'
import * as events from '/events-service.js'
import { modules } from '/modules.js'

const { createApp, defineComponent, ref, computed, onMounted, onUnmounted, watch, nextTick } = await FallbackLoader.vue();

const main = getComponent('Main');

const App = defineComponent({
    components: {
        SwitchCheckbox: getComponent('common/SwitchCheckbox')
    },
    setup() {
        const routes = {};
        for (let module of modules.all) {
            if (module.path) {
                routes['/' + module.path] = module;
            }
        }

        const search = ref('');
        const searchInput = ref(null);
        const path = ref(window.location.hash);
        const module = computed(() => {
            const route = routes[path.value.slice(1) || '/'];
            if (route != null) {
                return route;
            } else {
                return null;
            }
        });
        const view = computed(() => {
            if (module.value != null) {
                return module.value.componentRef;
            } else {
                return main;
            }
        });

        window.addEventListener('hashchange', () => {
            path.value = window.location.hash;
        });

        const onFilterKeyDown = event => {
            // escape
            if (event.keyCode == 27) {
                search.value = '';
                onFilterInput();
            }
        };

        const onFilterInput = () => {
            events.trigger({
                type: 'filter',
                value: search.value
            });
        };

        const isMain = () => {
            return view.value == main;
        };

        const onEvent = event => {
            if (event.type == 'focus-filter') {
                setTimeout(() => {
                    if (searchInput.value != null) {
                        searchInput.value.focus();
                    }
                }, 100);
            }
        };

        watch(view, async (current) => {
            if (current == main) {
                await nextTick();
                onFilterInput();
            }
        });

        onMounted(() => {
            events.subscribe(onEvent);
            http.getText('/api/user').then(response => {
                document.title = response;
            });
        });

        onUnmounted(() => {
            events.unsubscribe(onEvent);
        });

        return {
            search,
            searchInput,
            path,
            module,
            view,

            isMain,
            onFilterKeyDown,
            onFilterInput
        };
    }
});

const app = createApp(App);
app.mount('#vue-app');