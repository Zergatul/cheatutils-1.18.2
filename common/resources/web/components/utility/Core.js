import * as http from '/http.js'
import { components } from '/components.js'

export function createComponent(template) {
    const args = {
        template: template,
        created() {
            http.get('/api/core').then(response => {
                this.config = response;
            });
        },
        data() {
            return {
                config: null
            };
        },
        methods: {
            update() {
                http.post('/api/core', this.config).then(response => {
                    this.config = response;
                    if (location.port != this.config.port) {
                        setTimeout(() => {
                            location.assign(location.protocol + '//' + location.hostname + ':' + this.config.port + '/');
                        }, 500);
                    }
                });
            }
        }
    };
    components.add(args, 'SwitchCheckbox');
    return args;
}