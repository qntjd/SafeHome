import { create } from 'zustand'
import { persist } from 'zustand/middleware'

// zustand 설치 필요
// npm install zustand

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  email: string | null
  nickname: string | null
  isLoggedIn: boolean
  login: (tokens: { accessToken: string; refreshToken: string; email: string; nickname: string }) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      email: null,
      nickname: null,
      isLoggedIn: false,

      login: ({ accessToken, refreshToken, email, nickname }) => {
        localStorage.setItem('accessToken', accessToken)
        localStorage.setItem('refreshToken', refreshToken)
        set({ accessToken, refreshToken, email, nickname, isLoggedIn: true })
      },

      logout: () => {
        localStorage.clear()
        set({ accessToken: null, refreshToken: null, email: null, nickname: null, isLoggedIn: false })
      },
    }),
    { name: 'auth-storage' }
  )
)