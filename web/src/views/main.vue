<template>
  <a-layout>
    <the-header></the-header>
    <a-layout>
      <the-sider></the-sider>
      <a-layout style="padding: 0 24px 24px">
        <a-breadcrumb style="margin: 16px 0">
          <a-breadcrumb-item>Home</a-breadcrumb-item>
          <a-breadcrumb-item>List</a-breadcrumb-item>
          <a-breadcrumb-item>App</a-breadcrumb-item>
        </a-breadcrumb>
        <a-layout-content
            :style="{ background: '#fff', padding: '24px', margin: 0, minHeight: '280px' }"
        >
          所有会员总数：{{count}}
        </a-layout-content>
      </a-layout>
    </a-layout>
  </a-layout>
</template>
<script>
import {defineComponent, ref} from 'vue';
import TheHeader from "@/components/the-header.vue";
import TheSider from "@/components/the-sider.vue";
import axios from "axios";
import {notification} from "ant-design-vue";
export default defineComponent({
  name:"main-view",
  components: {
    TheSider,
    TheHeader,
  },
  setup() {
    const count = ref(0);
    axios.get("/member/member/count").then((response) => {
      let data = response.data;
      if (data.success) {
        count.value = data.content;
      } else {
        notification.error({ description: data.message });
      }
    })
    return {
      count
    };
  },
});
</script>
<style>
#components-layout-demo-top-side-2 .logo {
  float: left;
  width: 120px;
  height: 31px;
  margin: 16px 24px 16px 0;
  background: rgba(255, 255, 255, 0.3);
}

.ant-row-rtl #components-layout-demo-top-side-2 .logo {
  float: right;
  margin: 16px 0 16px 24px;
}

.site-layout-background {
  background: #fff;
}
</style>