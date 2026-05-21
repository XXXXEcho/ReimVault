import axios from 'axios';

const http = axios.create({
  baseURL: '/api',
  withCredentials: true
});

export default http;
