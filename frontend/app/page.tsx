'use client'

import { useCallback, useState } from 'react'
import FlowOpsClean from '@/components/landing/flowops-clean'
import { StartupLoader } from '@/components/landing/startup-loader'

export default function Page() {
  const [loaded, setLoaded] = useState(false)
  const handleComplete = useCallback(() => setLoaded(true), [])

  return (
    <>
      {!loaded && <StartupLoader onComplete={handleComplete} />}
      <FlowOpsClean />
    </>
  )
}