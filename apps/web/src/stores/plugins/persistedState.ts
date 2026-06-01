import type { PiniaPluginContext, StateTree } from 'pinia'

export interface PersistOptions {
  key?: string
  storage?: Storage
  paths?: string[]
}

declare module 'pinia' {
  export interface DefineStoreOptionsBase<S, Store> {
    persist?: boolean | PersistOptions
  }
}

const isBrowser = typeof window !== 'undefined'

function pickState(state: StateTree, paths?: string[]) {
  if (!paths?.length) {
    return state
  }

  return paths.reduce<StateTree>((result, path) => {
    result[path] = state[path]
    return result
  }, {})
}

export function createPersistedState() {
  return ({ options, store }: PiniaPluginContext) => {
    const persist = options.persist

    if (!persist || !isBrowser) {
      return
    }

    const persistOptions = typeof persist === 'object' ? persist : {}
    const storage = persistOptions.storage ?? window.localStorage
    const key = persistOptions.key ?? `slideforge:${store.$id}`
    const rawState = storage.getItem(key)

    if (rawState) {
      store.$patch(JSON.parse(rawState))
    }

    store.$subscribe((_mutation, state) => {
      storage.setItem(key, JSON.stringify(pickState(state, persistOptions.paths)))
    })
  }
}
