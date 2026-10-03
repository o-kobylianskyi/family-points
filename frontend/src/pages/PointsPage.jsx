import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'

import { useAuth } from '../context/AuthContext'
import { getWorkspaceMembers } from '../api/workspaceApi'

import {
  earnPoints,
  getMemberBalance,
  getMemberPointHistory,
  penalizePoints,
  spendPoints,
} from '../api/pointsApi'

import PointOperationModal
  from '../components/PointOperationModal'

function PointsPage() {
  const { t, i18n } = useTranslation()

  const {
    currentUser,
    getAccessToken,
  } = useAuth()

  const [members, setMembers] = useState([])
  const [selectedMemberId, setSelectedMemberId] = useState(null)

  const [balance, setBalance] = useState(0)
  const [history, setHistory] = useState([])

  const [loadingMembers, setLoadingMembers] = useState(true)
  const [loadingPoints, setLoadingPoints] = useState(false)
  const [error, setError] = useState('')
  const [pointOperation, setPointOperation] =
    useState(null)

  const canManagePoints =
    currentUser.permissions.includes('MANAGE_POINTS')

  useEffect(() => {
    const loadMembers = async () => {
      try {
        setError('')

        const token = getAccessToken()

        const data = await getWorkspaceMembers(
          token,
          currentUser.workspaceId
        )

        setMembers(data)

        if (data.length > 0) {
          const currentMemberExists = data.some(
            (member) => member.id === currentUser.memberId
          )

          setSelectedMemberId(
            currentMemberExists
              ? currentUser.memberId
              : data[0].id
          )
        }
      } catch (error) {
        setError(error.message)
      } finally {
        setLoadingMembers(false)
      }
    }

    loadMembers()
  }, [currentUser.workspaceId, currentUser.memberId])

  useEffect(() => {
    if (!selectedMemberId) {
      return
    }

    const loadPoints = async () => {
      try {
        setLoadingPoints(true)
        setError('')

        const token = getAccessToken()

        const [balanceResponse, historyResponse] =
          await Promise.all([
            getMemberBalance(
              token,
              currentUser.workspaceId,
              selectedMemberId
            ),
            getMemberPointHistory(
              token,
              currentUser.workspaceId,
              selectedMemberId
            ),
          ])

        setBalance(balanceResponse.balance)
        setHistory(historyResponse)
      } catch (error) {
        setError(error.message)
      } finally {
        setLoadingPoints(false)
      }
    }

    loadPoints()
  }, [selectedMemberId, currentUser.workspaceId])


const refreshPoints = async () => {
  if (!selectedMemberId) {
    return
  }

  const token = getAccessToken()

  const [balanceResponse, historyResponse] =
    await Promise.all([
      getMemberBalance(
        token,
        currentUser.workspaceId,
        selectedMemberId
      ),
      getMemberPointHistory(
        token,
        currentUser.workspaceId,
        selectedMemberId
      ),
    ])

  setBalance(balanceResponse.balance)
  setHistory(historyResponse)
}

  const selectedMember = members.find(
    (member) => member.id === selectedMemberId
  )

  const getInitials = (name) => {
    if (!name) {
      return '?'
    }

    return name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part[0])
      .join('')
      .toUpperCase()
  }

  const formatDate = (value) => {
    if (!value) {
      return ''
    }

    return new Intl.DateTimeFormat(
      i18n.resolvedLanguage || i18n.language,
      {
        dateStyle: 'medium',
        timeStyle: 'short',
      }
    ).format(new Date(value))
  }

  const getAmountClass = (amount) => {
    if (amount > 0) {
      return 'positive'
    }

    if (amount < 0) {
      return 'negative'
    }

    return ''
  }

const handlePointOperation = async ({
  operation,
  amount,
  description,
}) => {
  const token = getAccessToken()

  const args = [
    token,
    currentUser.workspaceId,
    selectedMemberId,
    amount,
    description,
  ]

  switch (operation) {
    case 'earn':
      await earnPoints(...args)
      break

    case 'spend':
      await spendPoints(...args)
      break

    case 'penalty':
      await penalizePoints(...args)
      break

    default:
      throw new Error('Unknown point operation')
  }

  await refreshPoints()
}

  const formatAmount = (amount) => {
    if (amount > 0) {
      return `+${amount}`
    }

    return String(amount)
  }

  if (loadingMembers) {
    return (
      <div className="page-loading">
        {t('common.loading')}
      </div>
    )
  }

  return (
    <>
      <div className="page-title">
        <h1>{t('points.title')}</h1>
        <p>{t('points.description')}</p>
      </div>

      {error && (
        <div className="page-error">
          {error}
        </div>
      )}

      <div className="member-selector">
        {members.map((member) => (
          <button
            key={member.id}
            type="button"
            className={
              member.id === selectedMemberId
                ? 'member-selector-button active'
                : 'member-selector-button'
            }
            onClick={() =>
              setSelectedMemberId(member.id)
            }
          >
            <span className="selector-avatar">
              {getInitials(member.name)}
            </span>

            <span>
              <strong>{member.name}</strong>

              <small>
                {t(`memberType.${member.memberType}`)}
              </small>
            </span>
          </button>
        ))}
      </div>

      {selectedMember && (
        <section className="points-summary-card">
          <div className="points-summary-member">
            <div className="points-large-avatar">
              {getInitials(selectedMember.name)}
            </div>

            <div>
              <h2>{selectedMember.name}</h2>

              <p>
                {t(
                  `memberType.${selectedMember.memberType}`
                )}
              </p>
            </div>
          </div>

          <div className="points-summary-right">
            <div className="points-current-balance">
              <strong>
                {loadingPoints ? '—' : balance}
              </strong>

              <span>{t('common.points')}</span>
            </div>

            {canManagePoints && (
              <div className="point-actions">
                <button
                  type="button"
                  className="point-action earn"
                   onClick={() => setPointOperation('earn')}
                >
                  + {t('points.earn')}
                </button>

                <button
                  type="button"
                  className="point-action spend"
                  onClick={() => setPointOperation('spend')}
                >
                  − {t('points.spend')}
                </button>

                <button
                  type="button"
                  className="point-action penalty"
                  onClick={() => setPointOperation('penalty')}
                >
                  {t('points.penalty')}
                </button>
              </div>
            )}
          </div>
        </section>
      )}

      <section className="points-history-card">
        <div className="panel-header">
          <div>
            <h2>{t('points.history')}</h2>
            <p>{t('points.historyDescription')}</p>
          </div>
        </div>

        {loadingPoints ? (
          <div className="empty-state">
            {t('common.loading')}
          </div>
        ) : history.length === 0 ? (
          <div className="empty-state">
            {t('points.noHistory')}
          </div>
        ) : (
          <div className="transaction-list">
            {history.map((transaction) => (
              <div
                className="transaction-row"
                key={transaction.id}
              >
                <div className="transaction-main">
                  <div className="transaction-type">
                    {t(
                      `pointTransactionType.${transaction.type}`,
                      {
                        defaultValue: transaction.type,
                      }
                    )}
                  </div>

                  <div className="transaction-description">
                    {transaction.description ||
                      t('points.noDescription')}
                  </div>

                  <div className="transaction-date">
                    {formatDate(transaction.createdAt)}
                  </div>
                </div>

                <div
                  className={`transaction-amount ${getAmountClass(
                    transaction.amount
                  )}`}
                >
                  {formatAmount(transaction.amount)}

                  <span>
                    {transaction.pointTypeName ||
                      t('common.points')}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
      <PointOperationModal
        operation={pointOperation}
        member={selectedMember}
        onClose={() => setPointOperation(null)}
        onSubmit={handlePointOperation}
      />
    </>
  )
}

export default PointsPage