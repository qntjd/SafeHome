import { useForm } from 'react-hook-form'
import { useAuth } from '@/hooks/useAuth'
import { Link } from 'react-router-dom'
import { useCurrentLocation } from '@/hooks/useCurrentLocation'
import { useState } from 'react'
import api from '@/api/axios'

interface FormData {
  email: string
  password: string
  nickname: string
}

export default function RegisterPage() {
  const { register, handleSubmit, watch, formState: { errors, isSubmitting } } = useForm<FormData>()
  const { handleRegister } = useAuth()
  const { position } = useCurrentLocation()
  const [error, setError] = useState('')
  const [codeSent, setCodeSent] = useState(false)
  const [verifyCode, setVerifyCode] = useState('')
  const [isVerified, setIsVerified] = useState(false)
  const [sending, setSending] = useState(false)
  const [verifying, setVerifying] = useState(false)

  const email = watch('email')

  const sendCode = async () => {
    if (!email) return setError('이메일을 입력해주세요.')
    setSending(true)
    try {
      await api.post('/auth/email/send', { email })
      setCodeSent(true)
      setError('')
    } catch {
      setError('인증코드 발송에 실패했습니다.')
    } finally {
      setSending(false)
    }
  }

  const verifyEmail = async () => {
    if (!verifyCode) return setError('인증코드를 입력해주세요.')
    setVerifying(true)
    try {
      const res = await api.post('/auth/email/verify', { email, code: verifyCode })
      if (res.data?.data) {
        setIsVerified(true)
        setError('')
      } else {
        setError('인증코드가 올바르지 않습니다.')
      }
    } catch {
      setError('인증 확인에 실패했습니다.')
    } finally {
      setVerifying(false)
    }
  }

  const onSubmit = async ({ email, password, nickname }: FormData) => {
    if (!isVerified) return setError('이메일 인증을 완료해주세요.')
    try {
      setError('')
      await handleRegister(email, password, nickname, position.lat, position.lng)
    } catch {
      setError('회원가입에 실패했습니다. 이미 사용 중인 이메일일 수 있어요.')
    }
  }

  const inputStyle = {
    background: 'var(--bg-hover)',
    border: '1px solid var(--border)',
    color: 'var(--text-primary)',
  }

  return (
    <div
      className="min-h-screen flex items-center justify-center px-4"
      style={{ background: 'var(--bg-primary)' }}
    >
      <div
        className="fixed top-0 left-1/2 -translate-x-1/2 w-96 h-96 rounded-full opacity-10 blur-3xl pointer-events-none"
        style={{ background: 'var(--accent-blue)' }}
      />

      <div className="w-full max-w-sm relative">
        <div className="text-center mb-8">
          <div
            className="w-12 h-12 rounded-2xl flex items-center justify-center text-xl font-bold mx-auto mb-4"
            style={{ background: 'var(--accent-blue)' }}
          >
            S
          </div>
          <h1 className="text-2xl font-bold" style={{ color: 'var(--text-primary)' }}>
            회원가입
          </h1>
          <p className="text-sm mt-1" style={{ color: 'var(--text-muted)' }}>
            현재 위치가 집 위치로 자동 등록됩니다.
          </p>
        </div>

        <div
          className="rounded-2xl p-6"
          style={{ background: 'var(--bg-card)', border: '1px solid var(--border)' }}
        >
          <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">

            {/* 이메일 + 인증 버튼 */}
            <div>
              <label className="text-xs font-medium block mb-1.5" style={{ color: 'var(--text-secondary)' }}>
                이메일
              </label>
              <div className="flex gap-2">
                <input
                  type="email"
                  placeholder="email@example.com"
                  className="flex-1 rounded-xl px-4 py-3 text-sm outline-none"
                  style={inputStyle}
                  disabled={isVerified}
                  {...register('email', { required: '이메일을 입력해주세요.' })}
                />
                {!isVerified && (
                  <button
                    type="button"
                    onClick={sendCode}
                    disabled={sending}
                    className="rounded-xl px-4 py-3 text-sm font-medium shrink-0 disabled:opacity-50"
                    style={{ background: 'var(--accent-blue)', color: '#fff' }}
                  >
                    {sending ? '발송 중...' : codeSent ? '재발송' : '인증'}
                  </button>
                )}
              </div>
              {errors.email && (
                <p className="text-xs mt-1" style={{ color: 'var(--accent-red)' }}>
                  {errors.email.message}
                </p>
              )}
            </div>

            {/* 인증코드 입력 */}
            {codeSent && !isVerified && (
              <div>
                <label className="text-xs font-medium block mb-1.5" style={{ color: 'var(--text-secondary)' }}>
                  인증코드
                </label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    placeholder="6자리 코드 입력"
                    maxLength={6}
                    value={verifyCode}
                    onChange={e => setVerifyCode(e.target.value)}
                    className="flex-1 rounded-xl px-4 py-3 text-sm outline-none"
                    style={inputStyle}
                  />
                  <button
                    type="button"
                    onClick={verifyEmail}
                    disabled={verifying}
                    className="rounded-xl px-4 py-3 text-sm font-medium shrink-0 disabled:opacity-50"
                    style={{ background: '#34D399', color: '#fff' }}
                  >
                    {verifying ? '확인 중...' : '확인'}
                  </button>
                </div>
              </div>
            )}

            {/* 인증 완료 표시 */}
            {isVerified && (
              <div
                className="rounded-xl px-4 py-2.5 text-sm"
                style={{ background: 'rgba(52,211,153,0.1)', color: '#34D399' }}
              >
                ✅ 이메일 인증 완료
              </div>
            )}

            {/* 닉네임 */}
            <div>
              <label className="text-xs font-medium block mb-1.5" style={{ color: 'var(--text-secondary)' }}>
                닉네임
              </label>
              <input
                placeholder="홍길동"
                className="w-full rounded-xl px-4 py-3 text-sm outline-none"
                style={inputStyle}
                {...register('nickname', { required: '닉네임을 입력해주세요.' })}
              />
              {errors.nickname && (
                <p className="text-xs mt-1" style={{ color: 'var(--accent-red)' }}>
                  {errors.nickname.message}
                </p>
              )}
            </div>

            {/* 비밀번호 */}
            <div>
              <label className="text-xs font-medium block mb-1.5" style={{ color: 'var(--text-secondary)' }}>
                비밀번호
              </label>
              <input
                type="password"
                placeholder="8자 이상 입력해주세요"
                className="w-full rounded-xl px-4 py-3 text-sm outline-none"
                style={inputStyle}
                {...register('password', {
                  required: '비밀번호를 입력해주세요.',
                  minLength: { value: 8, message: '8자 이상 입력해주세요.' },
                })}
              />
              {errors.password && (
                <p className="text-xs mt-1" style={{ color: 'var(--accent-red)' }}>
                  {errors.password.message}
                </p>
              )}
            </div>

            {error && (
              <div
                className="rounded-xl px-4 py-3 text-sm"
                style={{
                  background: 'rgba(248,113,113,0.1)',
                  color: 'var(--accent-red)',
                  border: '1px solid rgba(248,113,113,0.2)',
                }}
              >
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={isSubmitting || !isVerified}
              className="w-full rounded-xl py-3 text-sm font-medium transition-all disabled:opacity-50"
              style={{ background: 'var(--accent-blue)', color: '#fff' }}
            >
              {isSubmitting ? '가입 중...' : '가입하기'}
            </button>
          </form>

          <p className="text-center text-sm mt-4" style={{ color: 'var(--text-muted)' }}>
            이미 계정이 있으신가요?{' '}
            <Link to="/login" style={{ color: 'var(--accent-blue)' }}>
              로그인
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}