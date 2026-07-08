import React from 'react'

export default function StatusStamp({ status }) {
  return <span className={`stamp stamp-${status}`}>{status}</span>
}
