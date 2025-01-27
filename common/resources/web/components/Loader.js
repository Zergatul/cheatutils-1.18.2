import * as FallbackLoader from '/fallback-loader.js'
import * as http from '/http.js'

const { defineAsyncComponent } = await FallbackLoader.vue();

export function addComponent(args, name) {
    if (!args.components) {
        args.components = {};
    }
    args.components[name] = defineAsyncComponent(function () {
        return new Promise(function (resolve, reject) {
            http.getText(`/components/${name}.html`).then(response => {
                import(`/components/${name}.js`).then(function (module) {
                    resolve(module.createComponent(response));
                }, reject);
            }, reject);
        });
    });
};

export function getComponent(path) {
    return defineAsyncComponent(async () => {
        const html = await http.getText(`/components/${path}.html`);
        const module = await import(`/components/${path}.js`);
        const component = module.createComponent(html);
        if (component instanceof Promise) {
            return await component;
        } else {
            return component;
        }
    });
}

export function withCss(url, args) {
    const linkId = url.match(/(\w+)\.js$/)[1] + '-CSS';
    if (document.getElementById(linkId) != null) {
        return args;
    } else {
        return new Promise((resolve, reject) => {
            const link = document.createElement('link');
            link.href = url.replace(/\.js$/, '.css');
            link.rel = 'stylesheet';
            link.id = linkId;
            const onLoad = () => {
                clearEvents();
                resolve(args);
            };
            const onError = () => {
                clearEvents();
                reject(`Cannot load ${link.href}`);
            };
            const clearEvents = () => {
                link.removeEventListener('load', onLoad);
                link.removeEventListener('error', onError);
            };
            link.addEventListener('load', onLoad);
            link.addEventListener('error', onError);
            document.head.appendChild(link);
        });
    }
}