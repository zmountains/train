import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import DatePicker from 'ant-design-vue';
import 'ant-design-vue/dist/reset.css';
import * as Icons from '@ant-design/icons-vue';
import axios from 'axios';

const app = createApp(App);
app.use(DatePicker).use(store).use(router).mount('#app');


//全局使用图标
const icons = Icons;
for (const i in icons) {
    app.component(i,icons[i]);
}

/**
 * axios拦截器
 */
axios.interceptors.request.use(function (config) {
    console.log('请求参数：', config);
    return config;
}, error => {
    return Promise.reject(error);
});
axios.interceptors.response.use(function (response) {
    console.log('返回结果：', response);
    return response;
}, error => {
    console.log('返回错误：', error);
    return Promise.reject(error);
});
