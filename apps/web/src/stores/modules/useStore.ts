import { defineStore } from 'pinia'

interface UserState {
  username: string
  token: string
}

export const useStore = defineStore('user', {
  state: (): UserState => ({
    username: '',
    token: '',
  }),
  getters: {
    isLogin: (state) => Boolean(state.token),
  },
  actions: {
    setUser(username: string, token: string) {
      this.username = username
      this.token = token
    },
    setUsername(username: string) {
      this.username = username
    },
    setToken(token: string) {
      this.token = token
    },
    clearUser() {
      this.username = ''
      this.token = ''
    },
  },
  persist: {
    key: 'slideforge:user',
    paths: ['username', 'token'],
  },
})
