import * as http from '/http.js'
import { handleCodeSave } from '/components/MonacoEditor.js'
import { components } from '../../components.js'

export function createComponent(template) {
    const args = {
        template: template,
        created() {
            http.get('/api/hitbox-size').then(response => {
                this.config = response;
                this.code = response.code;
            });
        },
        data() {
            return {
                code: '',
                config: null,
                refs: null,
                showRefs: false
            };
        },
        methods: {
            saveCode() {
                handleCodeSave('/api/hitbox-size-code', this.code);
            },
            showApiRef() {
                if (this.showRefs) {
                    this.showRefs = false;
                } else {
                    if (this.refs) {
                        this.showRefs = true;
                    } else {
                        http.get('/api/scripts-doc/HITBOX_SIZE').then(response => {
                            this.showRefs = true;
                            this.refs = response;
                        });
                    }
                }
            },
            update() {
                return http.post('/api/hitbox-size', this.config).then(response => {
                    this.config = response;
                });
            }
        }
    };
    components.add(args, 'Radio');
    components.add(args, 'SwitchCheckbox');
    components.add(args, 'ScriptEditor');
    return args;
}