<template>
  <a-button type="primary" @click="showModal">新增</a-button>
  <a-modal v-model:visible="visible" title="乘车人" @ok="handleOk"
           ok-text="确认" cancel-text="取消">
    <p>Some cotents...</p>
    <p>Some cotents...</p>
    <p>Some cotents...</p>
  </a-modal>
</template>
<script>
import {defineComponent, ref} from 'vue';

export default defineComponent({
  name:"the-welcome",
  setup() {
    const visible = ref(false);

    const showModal = () =>{
      visible.value = true;
    };

    const handleOk = e =>{
      console.log(e);
      visible.value = false;
    }
    return {
      visible,
      showModal,
      handleOk
    };
  },
});
</script>
<style>
</style>
