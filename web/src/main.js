import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import DatePicker from 'ant-design-vue';
import 'ant-design-vue/dist/reset.css';
import * as Icons from '@ant-design/icons-vue';

const app = createApp(App);
app.use(DatePicker).use(store).use(router).mount('#app');


//全局使用图标
const icons = Icons;
for (const i in icons) {
    app.component(i,icons[i]);
}
