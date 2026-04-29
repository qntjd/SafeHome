import { useState, useEffect, useRef, useCallback } from 'react'

const SOS_KEYWORDS = [
  '살려줘', '살려', '도와줘', '도움', '助けて',
  'help', 'sos', '위험', '비상', '신고'
]

export function useSosDetection(onDetected: () => void) {
  const [isListening, setIsListening]   = useState(false)
  const [transcript, setTranscript]     = useState('')
  const [isSupported, setIsSupported]   = useState(false)
  const recognitionRef                  = useRef<any>(null)

  useEffect(() => {
    const SpeechRecognition =
      (window as any).SpeechRecognition ||
      (window as any).webkitSpeechRecognition
    setIsSupported(!!SpeechRecognition)
  }, [])

  const startListening = useCallback(() => {
    const SpeechRecognition =
      (window as any).SpeechRecognition ||
      (window as any).webkitSpeechRecognition

    if (!SpeechRecognition) return

    const recognition = new SpeechRecognition()
    recognition.lang        = 'ko-KR'
    recognition.continuous  = true
    recognition.interimResults = true

    recognition.onresult = (event: any) => {
      const text = Array.from(event.results)
        .map((r: any) => r[0].transcript)
        .join('')
        .toLowerCase()

      setTranscript(text)

      const detected = SOS_KEYWORDS.some(keyword =>
        text.includes(keyword.toLowerCase())
      )

      if (detected) {
        onDetected()
      }
    }

    recognition.onend = () => {
      // 자동 재시작 (연속 감지)
      if (isListening) {
        recognition.start()
      }
    }

    recognition.onerror = (e: any) => {
      console.warn('[SOS] 음성 감지 오류:', e.error)
      if (e.error !== 'no-speech') {
        setIsListening(false)
      }
    }

    recognitionRef.current = recognition
    recognition.start()
    setIsListening(true)
  }, [isListening, onDetected])

  const stopListening = useCallback(() => {
    recognitionRef.current?.stop()
    setIsListening(false)
    setTranscript('')
  }, [])

  return { isListening, transcript, isSupported, startListening, stopListening }
}