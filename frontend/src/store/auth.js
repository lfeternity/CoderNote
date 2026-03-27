import { defineStore } from 'pinia'
import { getProfile } from '../api/user'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    profile: null,
    initialized: false
  }),
  getters: {
    isLoggedIn: (state) => !!state.profile
  },
  actions: {
    setProfile(profile) {
      this.profile = profile
      localStorage.setItem('eb_logged_in', '1')
    },
    clearProfile() {
      this.profile = null
      localStorage.removeItem('eb_logged_in')
    },
    async refreshProfile() {
      const profile = await getProfile()
      this.profile = profile
      localStorage.setItem('eb_logged_in', '1')
      return profile
    },
    async bootstrap() {
      if (this.initialized) {
        return
      }
      if (localStorage.getItem('eb_logged_in') === '1') {
        try {
          await this.refreshProfile()
        } catch (error) {
          this.clearProfile()
        }
      }
      this.initialized = true
    }
  }
})