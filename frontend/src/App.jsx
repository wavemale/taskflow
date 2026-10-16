import { useEffect, useState } from 'react'
import './App.css'

const API_URL = 'http://localhost:8080'

const emptyTask = {
  title: '',
  description: '',
  deadline: '',
  importance: 'MEDIUM',
  status: 'TODO',
}

function App() {
  const [isRegister, setIsRegister] = useState(false)
  const [isAuthenticated, setIsAuthenticated] = useState(
      Boolean(localStorage.getItem('token'))
  )

  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  const [tasks, setTasks] = useState([])
  const [statistics, setStatistics] = useState(null)

  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [importanceFilter, setImportanceFilter] = useState('')
  const [sort, setSort] = useState('priority')

  const [showTaskForm, setShowTaskForm] = useState(false)
  const [editingTaskId, setEditingTaskId] = useState(null)
  const [taskForm, setTaskForm] = useState(emptyTask)
  const [taskLoading, setTaskLoading] = useState(false)

  const resetMessages = () => {
    setError('')
    setMessage('')
  }

  const switchMode = () => {
    setIsRegister(!isRegister)

    setUsername('')
    setEmail('')
    setPassword('')

    resetMessages()
  }

  const handleAuthSubmit = async (event) => {
    event.preventDefault()

    resetMessages()

    if (!email.trim() || !password.trim()) {
      setError('Введите email и пароль')
      return
    }

    if (isRegister && !username.trim()) {
      setError('Введите имя пользователя')
      return
    }

    setLoading(true)

    try {
      const url = isRegister
          ? `${API_URL}/auth/register`
          : `${API_URL}/auth/login`

      const body = isRegister
          ? {
            username: username.trim(),
            email: email.trim(),
            password,
          }
          : {
            email: email.trim(),
            password,
          }

      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(body),
      })

      const contentType = response.headers.get('content-type')

      let data

      if (contentType?.includes('application/json')) {
        data = await response.json()
      } else {
        data = await response.text()
      }

      if (!response.ok) {
        if (typeof data === 'object' && data !== null) {
          throw new Error(
              data.error ||
              data.message ||
              'Произошла ошибка'
          )
        }

        throw new Error(
            data || 'Произошла ошибка'
        )
      }

      if (isRegister) {
        setMessage(
            'Регистрация прошла успешно. Теперь войдите в аккаунт.'
        )

        setUsername('')
        setPassword('')
        setIsRegister(false)
      } else {
        if (!data?.token) {
          throw new Error(
              'Сервер не вернул JWT-токен'
          )
        }

        localStorage.setItem(
            'token',
            data.token
        )

        setIsAuthenticated(true)
        setPassword('')
      }
    } catch (err) {
      setError(
          err.message ||
          'Не удалось выполнить запрос'
      )
    } finally {
      setLoading(false)
    }
  }

  const logout = () => {
    localStorage.removeItem('token')

    setIsAuthenticated(false)
    setTasks([])
    setStatistics(null)
    setEmail('')
    setPassword('')
    resetMessages()
  }

  const loadTasks = async () => {
    const token = localStorage.getItem('token')

    if (!token) {
      setIsAuthenticated(false)
      return
    }

    try {
      const params = new URLSearchParams()

      if (search.trim()) {
        params.append(
            'search',
            search.trim()
        )
      }

      if (statusFilter) {
        params.append(
            'status',
            statusFilter
        )
      }

      if (importanceFilter) {
        params.append(
            'importance',
            importanceFilter
        )
      }

      if (sort) {
        params.append('sort', sort)
      }

      const query = params.toString()

      const response = await fetch(
          `${API_URL}/tasks${query ? `?${query}` : ''}`,
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
      )

      if (response.status === 401) {
        logout()
        return
      }

      if (!response.ok) {
        throw new Error(
            'Не удалось загрузить задачи'
        )
      }

      const data = await response.json()

      setTasks(data)
    } catch (err) {
      setError(
          err.message ||
          'Не удалось загрузить задачи'
      )
    }
  }

  const loadStatistics = async () => {
    const token = localStorage.getItem('token')

    if (!token) {
      return
    }

    try {
      const response = await fetch(
          `${API_URL}/tasks/statistics`,
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
      )

      if (response.status === 401) {
        logout()
        return
      }

      if (!response.ok) {
        throw new Error(
            'Не удалось загрузить статистику'
        )
      }

      const data = await response.json()

      setStatistics(data)
    } catch (err) {
      console.error(err)
    }
  }

  useEffect(() => {
    if (!isAuthenticated) {
      return
    }

    loadTasks()
    loadStatistics()
  }, [
    isAuthenticated,
    search,
    statusFilter,
    importanceFilter,
    sort,
  ])

  const openCreateForm = () => {
    setEditingTaskId(null)
    setTaskForm({
      ...emptyTask,
    })
    setError('')
    setShowTaskForm(true)
  }

  const openEditForm = (task) => {
    setEditingTaskId(task.id)

    setTaskForm({
      title: task.title || '',
      description: task.description || '',
      deadline: task.deadline || '',
      importance:
          task.importance || 'MEDIUM',
      status:
          task.status || 'TODO',
    })

    setError('')
    setShowTaskForm(true)
  }

  const closeTaskForm = () => {
    setShowTaskForm(false)
    setEditingTaskId(null)
    setTaskForm({
      ...emptyTask,
    })
  }

  const handleTaskChange = (field, value) => {
    setTaskForm((previous) => ({
      ...previous,
      [field]: value,
    }))
  }

  const saveTask = async (event) => {
    event.preventDefault()

    if (!taskForm.title.trim()) {
      setError('Введите название задачи')
      return
    }

    if (!taskForm.deadline) {
      setError('Выберите дедлайн')
      return
    }

    setTaskLoading(true)
    setError('')

    const token = localStorage.getItem('token')

    try {
      const url = editingTaskId
          ? `${API_URL}/tasks/${editingTaskId}`
          : `${API_URL}/tasks`

      const response = await fetch(url, {
        method: editingTaskId
            ? 'PUT'
            : 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          title: taskForm.title.trim(),
          description:
              taskForm.description.trim(),
          deadline: taskForm.deadline,
          importance: taskForm.importance,
          status: taskForm.status,
        }),
      })

      const contentType =
          response.headers.get(
              'content-type'
          )

      let data

      if (contentType?.includes('application/json')) {
        data = await response.json()
      } else {
        data = await response.text()
      }

      if (response.status === 401) {
        logout()
        return
      }

      if (!response.ok) {
        if (
            typeof data === 'object' &&
            data !== null
        ) {
          throw new Error(
              data.error ||
              data.message ||
              'Не удалось сохранить задачу'
          )
        }

        throw new Error(
            data ||
            'Не удалось сохранить задачу'
        )
      }

      closeTaskForm()

      await loadTasks()
      await loadStatistics()
    } catch (err) {
      setError(
          err.message ||
          'Не удалось сохранить задачу'
      )
    } finally {
      setTaskLoading(false)
    }
  }

  const deleteTask = async (id) => {
    const confirmed = window.confirm(
        'Удалить эту задачу?'
    )

    if (!confirmed) {
      return
    }

    const token = localStorage.getItem('token')

    try {
      const response = await fetch(
          `${API_URL}/tasks/${id}`,
          {
            method: 'DELETE',
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
      )

      if (response.status === 401) {
        logout()
        return
      }

      if (!response.ok) {
        throw new Error(
            'Не удалось удалить задачу'
        )
      }

      await loadTasks()
      await loadStatistics()
    } catch (err) {
      setError(
          err.message ||
          'Не удалось удалить задачу'
      )
    }
  }

  const getImportanceLabel = (
      importance
  ) => {
    switch (importance) {
      case 'HIGH':
        return 'Высокая'
      case 'MEDIUM':
        return 'Средняя'
      case 'LOW':
        return 'Низкая'
      default:
        return importance
    }
  }

  const getStatusLabel = (status) => {
    switch (status) {
      case 'TODO':
        return 'К выполнению'
      case 'IN_PROGRESS':
        return 'В работе'
      case 'DONE':
        return 'Выполнено'
      default:
        return status
    }
  }

  const formatDate = (date) => {
    if (!date) {
      return 'Не указан'
    }

    const parts = date.split('-')

    if (parts.length !== 3) {
      return date
    }

    return `${parts[2]}.${parts[1]}.${parts[0]}`
  }

  if (!isAuthenticated) {
    return (
        <div className="app">
          <div className="auth-container">
            <div className="auth-card">

              <div className="logo">
                <div className="logo-icon">
                  T
                </div>

                <span>TaskFlow</span>
              </div>

              <div className="auth-header">
                <h1>
                  {isRegister
                      ? 'Создание аккаунта'
                      : 'С возвращением'}
                </h1>

                <p>
                  {isRegister
                      ? 'Создайте аккаунт для работы с задачами'
                      : 'Войдите, чтобы продолжить работу с задачами'}
                </p>
              </div>

              <form
                  onSubmit={handleAuthSubmit}
                  className="auth-form"
              >

                {isRegister && (
                    <div className="form-group">
                      <label htmlFor="username">
                        Имя пользователя
                      </label>

                      <input
                          id="username"
                          type="text"
                          placeholder="Введите имя пользователя"
                          value={username}
                          onChange={(event) =>
                              setUsername(
                                  event.target.value
                              )
                          }
                          autoComplete="username"
                      />
                    </div>
                )}

                <div className="form-group">
                  <label htmlFor="email">
                    Email
                  </label>

                  <input
                      id="email"
                      type="email"
                      placeholder="Введите email"
                      value={email}
                      onChange={(event) =>
                          setEmail(
                              event.target.value
                          )
                      }
                      autoComplete="email"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="password">
                    Пароль
                  </label>

                  <input
                      id="password"
                      type="password"
                      placeholder="Введите пароль"
                      value={password}
                      onChange={(event) =>
                          setPassword(
                              event.target.value
                          )
                      }
                      autoComplete={
                        isRegister
                            ? 'new-password'
                            : 'current-password'
                      }
                  />
                </div>

                {error && (
                    <div className="error-message">
                      {error}
                    </div>
                )}

                {message && (
                    <div className="success-message">
                      {message}
                    </div>
                )}

                <button
                    type="submit"
                    className="auth-button"
                    disabled={loading}
                >
                  {loading
                      ? isRegister
                          ? 'Регистрация...'
                          : 'Вход...'
                      : isRegister
                          ? 'Зарегистрироваться'
                          : 'Войти'}
                </button>
              </form>

              <div className="switch-auth">
              <span>
                {isRegister
                    ? 'Уже есть аккаунт?'
                    : 'Нет аккаунта?'}
              </span>

                <button
                    type="button"
                    className="switch-button"
                    onClick={switchMode}
                >
                  {isRegister
                      ? 'Войти'
                      : 'Зарегистрироваться'}
                </button>
              </div>

            </div>
          </div>
        </div>
    )
  }

  return (
      <div className="dashboard">

        <header className="topbar">
          <div className="topbar-logo">
            <div className="logo-icon">
              T
            </div>

            <span>TaskFlow</span>
          </div>

          <div className="user-area">
          <span className="user-email">
            {email ||
                'Авторизованный пользователь'}
          </span>

            <button
                type="button"
                className="logout-button"
                onClick={logout}
            >
              Выйти
            </button>
          </div>
        </header>

        <main className="dashboard-content">

          <div className="page-header">
            <div>
              <h1>Мои задачи</h1>
              <p>
                Управляйте задачами и следите
                за их приоритетами
              </p>
            </div>

            <button
                type="button"
                className="primary-button"
                onClick={openCreateForm}
            >
              + Новая задача
            </button>
          </div>

          {error && (
              <div className="dashboard-error">
                {error}
              </div>
          )}

          {statistics && (
              <div className="statistics-grid">

                <div className="stat-card">
              <span className="stat-label">
                Всего задач
              </span>

                  <strong>
                    {statistics.totalTasks}
                  </strong>
                </div>

                <div className="stat-card">
              <span className="stat-label">
                Выполнено
              </span>

                  <strong>
                    {statistics.completedTasks}
                  </strong>
                </div>

                <div className="stat-card">
              <span className="stat-label">
                В работе
              </span>

                  <strong>
                    {statistics.inProgressTasks}
                  </strong>
                </div>

                <div className="stat-card">
              <span className="stat-label">
                Просрочено
              </span>

                  <strong>
                    {statistics.overdueTasks}
                  </strong>
                </div>

                <div className="stat-card">
              <span className="stat-label">
                Выполнено, %
              </span>

                  <strong>
                    {Math.round(
                        statistics.completionPercentage
                    )}
                    %
                  </strong>
                </div>

              </div>
          )}

          <div className="filters">

            <input
                type="text"
                className="search-input"
                placeholder="Поиск задачи..."
                value={search}
                onChange={(event) =>
                    setSearch(
                        event.target.value
                    )
                }
            />

            <select
                value={statusFilter}
                onChange={(event) =>
                    setStatusFilter(
                        event.target.value
                    )
                }
            >
              <option value="">
                Все статусы
              </option>

              <option value="TODO">
                К выполнению
              </option>

              <option value="IN_PROGRESS">
                В работе
              </option>

              <option value="DONE">
                Выполнено
              </option>
            </select>

            <select
                value={importanceFilter}
                onChange={(event) =>
                    setImportanceFilter(
                        event.target.value
                    )
                }
            >
              <option value="">
                Любая важность
              </option>

              <option value="HIGH">
                Высокая
              </option>

              <option value="MEDIUM">
                Средняя
              </option>

              <option value="LOW">
                Низкая
              </option>
            </select>

            <select
                value={sort}
                onChange={(event) =>
                    setSort(event.target.value)
                }
            >
              <option value="priority">
                По приоритету
              </option>

              <option value="deadline">
                По дедлайну
              </option>

              <option value="created">
                По дате создания
              </option>

              <option value="title">
                По названию
              </option>
            </select>

          </div>

          <div className="tasks-section">

            <div className="section-header">
              <h2>
                Задачи
              </h2>

              <span>
              {tasks.length}{' '}
                {tasks.length === 1
                    ? 'задача'
                    : 'задач'}
            </span>
            </div>

            {tasks.length === 0 ? (
                <div className="empty-state">
                  <h3>
                    Задач пока нет
                  </h3>

                  <p>
                    Создайте первую задачу,
                    чтобы начать работу.
                  </p>

                  <button
                      type="button"
                      className="primary-button"
                      onClick={openCreateForm}
                  >
                    Создать задачу
                  </button>
                </div>
            ) : (
                <div className="task-list">

                  {tasks.map((task) => (
                      <div
                          className="task-card"
                          key={task.id}
                      >

                        <div className="task-main">

                          <div className="task-title-row">
                            <h3>
                              {task.title}
                            </h3>

                            <span
                                className={`priority-score ${
                                    task.priorityScore >= 70
                                        ? 'priority-high'
                                        : task.priorityScore >= 40
                                            ? 'priority-medium'
                                            : 'priority-low'
                                }`}
                            >
                        {task.priorityScore ?? 0}
                      </span>
                          </div>

                          {task.description && (
                              <p className="task-description">
                                {task.description}
                              </p>
                          )}

                          <div className="task-meta">

                      <span>
                        Важность:{' '}
                        <strong>
                          {getImportanceLabel(
                              task.importance
                          )}
                        </strong>
                      </span>

                            <span>
                        Статус:{' '}
                              <strong>
                          {getStatusLabel(
                              task.status
                          )}
                        </strong>
                      </span>

                            <span>
                        Дедлайн:{' '}
                              <strong>
                          {formatDate(
                              task.deadline
                          )}
                        </strong>
                      </span>

                          </div>

                        </div>

                        <div className="task-actions">

                          <button
                              type="button"
                              className="edit-button"
                              onClick={() =>
                                  openEditForm(task)
                              }
                          >
                            Изменить
                          </button>

                          <button
                              type="button"
                              className="delete-button"
                              onClick={() =>
                                  deleteTask(task.id)
                              }
                          >
                            Удалить
                          </button>

                        </div>

                      </div>
                  ))}

                </div>
            )}

          </div>

        </main>

        {showTaskForm && (
            <div className="modal-overlay">

              <div className="modal">

                <div className="modal-header">
                  <div>
                    <h2>
                      {editingTaskId
                          ? 'Редактирование задачи'
                          : 'Новая задача'}
                    </h2>

                    <p>
                      Заполните данные задачи
                    </p>
                  </div>

                  <button
                      type="button"
                      className="close-button"
                      onClick={closeTaskForm}
                  >
                    ×
                  </button>
                </div>

                <form
                    className="task-form"
                    onSubmit={saveTask}
                >

                  <div className="form-group">
                    <label htmlFor="task-title">
                      Название
                    </label>

                    <input
                        id="task-title"
                        type="text"
                        placeholder="Например, подготовить отчёт"
                        value={taskForm.title}
                        onChange={(event) =>
                            handleTaskChange(
                                'title',
                                event.target.value
                            )
                        }
                        maxLength={100}
                    />
                  </div>

                  <div className="form-group">
                    <label htmlFor="task-description">
                      Описание
                    </label>

                    <textarea
                        id="task-description"
                        placeholder="Описание задачи"
                        value={
                          taskForm.description
                        }
                        onChange={(event) =>
                            handleTaskChange(
                                'description',
                                event.target.value
                            )
                        }
                        maxLength={500}
                        rows={4}
                    />
                  </div>

                  <div className="form-row">

                    <div className="form-group">
                      <label htmlFor="task-deadline">
                        Дедлайн
                      </label>

                      <input
                          id="task-deadline"
                          type="date"
                          value={
                            taskForm.deadline
                          }
                          onChange={(event) =>
                              handleTaskChange(
                                  'deadline',
                                  event.target.value
                              )
                          }
                      />
                    </div>

                    <div className="form-group">
                      <label htmlFor="task-importance">
                        Важность
                      </label>

                      <select
                          id="task-importance"
                          value={
                            taskForm.importance
                          }
                          onChange={(event) =>
                              handleTaskChange(
                                  'importance',
                                  event.target.value
                              )
                          }
                      >
                        <option value="LOW">
                          Низкая
                        </option>

                        <option value="MEDIUM">
                          Средняя
                        </option>

                        <option value="HIGH">
                          Высокая
                        </option>
                      </select>
                    </div>

                  </div>

                  <div className="form-group">
                    <label htmlFor="task-status">
                      Статус
                    </label>

                    <select
                        id="task-status"
                        value={taskForm.status}
                        onChange={(event) =>
                            handleTaskChange(
                                'status',
                                event.target.value
                            )
                        }
                    >
                      <option value="TODO">
                        К выполнению
                      </option>

                      <option value="IN_PROGRESS">
                        В работе
                      </option>

                      <option value="DONE">
                        Выполнено
                      </option>
                    </select>
                  </div>

                  {error && (
                      <div className="error-message">
                        {error}
                      </div>
                  )}

                  <div className="modal-actions">

                    <button
                        type="button"
                        className="secondary-button"
                        onClick={closeTaskForm}
                    >
                      Отмена
                    </button>

                    <button
                        type="submit"
                        className="primary-button"
                        disabled={taskLoading}
                    >
                      {taskLoading
                          ? 'Сохранение...'
                          : editingTaskId
                              ? 'Сохранить изменения'
                              : 'Создать задачу'}
                    </button>

                  </div>

                </form>

              </div>

            </div>
        )}

      </div>
  )
}

export default App