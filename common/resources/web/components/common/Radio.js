import * as FallbackLoader from '/fallback-loader.js'
import { withCss } from '/components/Loader.js'

const { ref, computed, toRefs } = await FallbackLoader.vue();

let index = 1;

export function createComponent(template) {
    const args = {
        template,
        props: {
            modelValue: {
                required: true
            },
            name: {
                type: String,
                required: true
            },
            value: {
                required: true
            }
        },
        setup(props, { emit }) {
            const id = ref('radio' + (index++));
            const { modelValue, name, value } = toRefs(props);

            const isChecked = computed(() => {
                return modelValue.value == value.value;
            });

            const onChange = event => {
                emit('update:modelValue', event.target.value);
            };

            return {
                id,
                name,
                value,
                isChecked,
                onChange
            };
        }
    };

    return withCss(import.meta.url, args);
}