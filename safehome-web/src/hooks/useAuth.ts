import { useAuthStore } from '@/store/authStore'
import { authApi } from '@/api/auth'
import { useNavigate } from 'react-router-dom'

export function useAuth() {
  const { login, logout, isLoggedIn, nickname, email } = useAuthStore()
  const navigate = useNavigate()

  const handleLogin = async (email: string, password: string) => {
    const { data } = await authApi.login({ email, password })
    login(data.data)
    navigate('/')
  }

  const handleRegister = async (
    email: string, password: string,
    nickname: string, homeLat?: number, homeLng?: number
  ) => {
    const { data } = await authApi.register({ email, password, nickname, homeLat, homeLng })
    login(data.data)
    navigate('/')
  }

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return { handleLogin, handleRegister, handleLogout, isLoggedIn, nickname, email }
}