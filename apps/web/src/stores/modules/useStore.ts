import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export const useStore = defineStore(
  'user',
  () => {
    const username = ref('')
    const token = ref('')

    const isLogin = computed(() => Boolean(token.value))

    function setUser(nextUsername: string, nextToken: string) {
      username.value = nextUsername
      token.value = nextToken
    }

    function setUsername(nextUsername: string) {
      username.value = nextUsername
    }

    function setToken(nextToken: string) {
      token.value = nextToken
    }

    function clearUser() {
      username.value = ''
      token.value = ''
    }

    return {
      username,
      token,
      isLogin,
      setUser,
      setUsername,
      setToken,
      clearUser,
    }
  },
  {
    persist: {
      key: 'slideforge:user',
      paths: ['username', 'token'],
    },
  },
)
