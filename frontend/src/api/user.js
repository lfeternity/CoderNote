import request from '../utils/request'

export function register(payload) {
  return request.post('/user/register', payload)
}

export function login(payload) {
  return request.post('/user/login', payload)
}

export function getRegisterCaptcha() {
  return request.get('/user/captcha')
}

export function logout() {
  return request.post('/user/logout')
}

export function getProfile() {
  return request.get('/user/profile')
}

export function updateProfile(payload) {
  return request.put('/user/profile', payload)
}

export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/avatar', formData)
}

export function resetAvatar() {
  return request.delete('/user/avatar')
}

export function changePassword(payload) {
  return request.put('/user/change-password', payload)
}
