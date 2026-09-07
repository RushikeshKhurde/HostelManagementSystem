import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: Automatically attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('hms_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: Handle 401 unauthorized & clean error messages
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      // If 401 Unauthorized and not on login page, clear token and redirect
      if (error.response.status === 401 && !window.location.pathname.includes('/login')) {
        localStorage.removeItem('hms_token');
        localStorage.removeItem('hms_user');
        window.location.href = '/login';
      }

      const resData = error.response.data;
      let message = 'An error occurred. Please try again.';

      if (resData) {
        if (resData.message) {
          message = resData.message;
        } else if (resData.errors && typeof resData.errors === 'object') {
          message = Object.values(resData.errors)[0] || message;
        } else if (typeof resData === 'string') {
          message = resData;
        }
      }

      return Promise.reject(new Error(message));
    } else if (error.request) {
      return Promise.reject(new Error('Cannot connect to server. Please check your backend connection.'));
    } else {
      return Promise.reject(error);
    }
  }
);

export default api;