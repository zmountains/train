<template>
  <a-layout-header id="components-layout-demo-top-side-2" class="header">
    <div class="logo">
      <router-link to="/welcome" style="color: white; font-size: 18px">
        甲蛙12306
      </router-link>
    </div>
    <div style="float: right; color: white;">
      您好：{{member.mobile}} &nbsp;&nbsp;
      <router-link to="/login" style="color: white;">
        退出登录
      </router-link>
    </div>
    <a-menu
        v-model:selectedKeys="selectedKeys1"
        theme="dark"
        mode="horizontal"
        :style="{ lineHeight: '64px' }"
    >
      <a-menu-item key="1">nav 111</a-menu-item>
      <a-menu-item key="2">nav 2</a-menu-item>
      <a-menu-item key="3">nav 3</a-menu-item>
    </a-menu>
  </a-layout-header>
</template>


<script>
  import { defineComponent, ref } from 'vue';
  import store from "@/store";


  export default defineComponent({
    name:"the-header",
    computed: {
      store() {
        return store
      }
    },
    components: {
    },
    setup() {
      let member = store.state.member;
      return {
        member,
        selectedKeys1: ref(['2']),
      };
    },
  });
</script>


