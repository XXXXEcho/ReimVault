<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ReimbursementForm from '../../components/ReimbursementForm.vue';

const route = useRoute();
const router = useRouter();
const id = computed(() => route.params.id ? Number(route.params.id) : undefined);

function back() {
  router.push('/reimbursements');
}
</script>

<template>
  <section>
    <h1>{{ id ? '编辑报销' : '新建报销' }}</h1>
    <ReimbursementForm :id="id" @submitted="back" />
  </section>
</template>
