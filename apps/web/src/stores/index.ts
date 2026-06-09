import { createPinia } from 'pinia'

import { createPersistedState } from './plugins/persistedState'

const pinia = createPinia()

pinia.use(createPersistedState())

export default pinia
export * from './modules/useAiSettingsStore'
export * from './modules/useOnePageDraftStore'
export * from './modules/useStore'
