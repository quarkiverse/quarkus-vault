import { QwcHotReloadElement, html, css } from 'qwc-hot-reload-element';
import { JsonRpc } from 'jsonrpc';

const DEFAULT_ENGINE = '<default>';
const DOCS_URL = 'https://docs.quarkiverse.io/quarkus-vault/dev/index.html';

export class QwcVaultConfigSources extends QwcHotReloadElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: block;
            padding: var(--lumo-space-m);
            color: var(--lumo-body-text-color);
            font-family: var(--lumo-font-family);
        }
        .path {
            margin-bottom: var(--lumo-space-m);
        }
        .meta {
            font-size: var(--lumo-font-size-s);
            color: var(--lumo-secondary-text-color);
        }
        code {
            display: block;
            font-family: var(--lumo-font-family-mono, monospace);
            font-size: var(--lumo-font-size-s);
        }
        .error {
            color: var(--lumo-error-text-color);
            font-family: var(--lumo-font-family-mono, monospace);
            font-size: var(--lumo-font-size-s);
            word-break: break-word;
        }
    `;

    static properties = {
        _snapshot: { state: true },
        _loading: { state: true }
    };

    constructor() {
        super();
        this._loading = true;
        this._snapshot = null;
    }

    connectedCallback() {
        super.connectedCallback();
        this.hotReload();
    }

    hotReload() {
        this._loading = true;
        this.jsonRpc.getConfigSources().then(response => {
            this._snapshot = response.result;
            this._loading = false;
        });
    }

    render() {
        if (this._loading) {
            return html`<vaadin-progress-bar indeterminate></vaadin-progress-bar>`;
        }
        if (!this._snapshot || this._snapshot.paths.length === 0) {
            return html`<p>No KV path is read by the Vault config source. Set <code>quarkus.vault.secret-config-kv-path</code> to load secrets as configuration properties. See the <a href="${DOCS_URL}" target="_blank">Vault extension documentation</a>.</p>`;
        }
        return html`
            <p class="meta">Fetched at ${this._snapshot.fetchedAt}</p>
            ${this._snapshot.paths.map(path => this._renderPath(path))}
        `;
    }

    _renderPath(path) {
        const details = [];
        if (path.engine !== DEFAULT_ENGINE) {
            details.push(`engine: ${path.engine}`);
        }
        if (path.prefix) {
            details.push(`prefix: ${path.prefix}`);
        }
        return html`
            <div class="path">
                <h4>${path.path}</h4>
                ${details.length > 0 ? html`<div class="meta">${details.join(' | ')}</div>` : ''}
                ${path.error
                    ? html`<div class="error">${path.error}</div>`
                    : path.propertyNames.length === 0
                        ? html`<div class="meta">No properties</div>`
                        : path.propertyNames.map(name => html`<code>${name}</code>`)}
            </div>
        `;
    }
}
customElements.define('qwc-vault-config-sources', QwcVaultConfigSources);
