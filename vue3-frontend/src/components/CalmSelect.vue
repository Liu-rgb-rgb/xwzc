<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue';

const props = defineProps<{ modelValue: string; options: string[]; ariaLabel?: string }>();
const emit = defineEmits<{ (event: 'update:modelValue', value: string): void }>();

const root = ref<HTMLElement | null>(null);
const trigger = ref<HTMLButtonElement | null>(null);
const menu = ref<HTMLElement | null>(null);
const open = ref(false);

function show() {
  open.value = true;
  nextTick(() => {
    const selected = menu.value?.querySelector<HTMLButtonElement>('button.on');
    (selected || menu.value?.querySelector<HTMLButtonElement>('button'))?.focus();
  });
}
function close() {
  open.value = false;
  trigger.value?.focus();
}
function toggle() {
  open.value ? close() : show();
}
function select(option: string) {
  emit('update:modelValue', option);
  open.value = false;
  trigger.value?.focus();
}
function menuButtons() {
  return [...(menu.value?.querySelectorAll<HTMLButtonElement>('button') || [])];
}
function onTriggerKeydown(event: KeyboardEvent) {
  if (event.key === 'ArrowDown' || event.key === 'ArrowUp' || event.key === 'Enter' || event.key === ' ') {
    event.preventDefault();
    if (!open.value) show();
  }
}
function onMenuKeydown(event: KeyboardEvent) {
  const buttons = menuButtons();
  const index = buttons.indexOf(document.activeElement as HTMLButtonElement);
  if (event.key === 'ArrowDown') {
    event.preventDefault();
    buttons[Math.min(buttons.length - 1, index + 1)]?.focus();
  } else if (event.key === 'ArrowUp') {
    event.preventDefault();
    buttons[Math.max(0, index - 1)]?.focus();
  } else if (event.key === 'Escape') {
    event.preventDefault();
    close();
  } else if (event.key === 'Tab') {
    open.value = false;
  }
}
function onDocumentPointerDown(event: PointerEvent) {
  if (!open.value) return;
  if (root.value && !root.value.contains(event.target as Node)) open.value = false;
}
function onDocumentKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && open.value) close();
}

onMounted(() => {
  document.addEventListener('pointerdown', onDocumentPointerDown);
  document.addEventListener('keydown', onDocumentKeydown);
});
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', onDocumentPointerDown);
  document.removeEventListener('keydown', onDocumentKeydown);
});
</script>

<template>
  <div
    ref="root"
    class="calm-select"
    :class="{ open }"
  >
    <button
      ref="trigger"
      type="button"
      class="calm-trigger"
      aria-haspopup="listbox"
      :aria-expanded="open"
      :aria-label="props.ariaLabel"
      @click="toggle"
      @keydown="onTriggerKeydown"
    >
      <span>{{ props.modelValue }}</span>
      <svg
        class="calm-chevron"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <polyline points="6 9 12 15 18 9" />
      </svg>
    </button>
    <Transition name="calm-pop">
      <ul
        v-if="open"
        ref="menu"
        class="calm-menu"
        role="listbox"
        :aria-label="props.ariaLabel"
        @keydown="onMenuKeydown"
      >
        <li
          v-for="option in props.options"
          :key="option"
        >
          <button
            type="button"
            role="option"
            :aria-selected="option === props.modelValue"
            :class="{ on: option === props.modelValue }"
            @click="select(option)"
          >
            <span>{{ option }}</span>
            <svg
              v-if="option === props.modelValue"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.4"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path d="m5 12 4.5 4.5L19 7" />
            </svg>
          </button>
        </li>
      </ul>
    </Transition>
  </div>
</template>

<style scoped>
.calm-select {
  position: relative;
  min-width: 0;
  color: #5f5c56;
  font-family: 'Noto Sans SC', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}
.calm-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  height: 100%;
  min-height: 50px;
  padding: 14px 18px 14px 20px;
  border: 1px solid #eae6df;
  border-radius: 12px;
  background: #fff;
  color: #5f5c56;
  font-size: 14px;
  text-align: left;
  transition:
    border-color 0.3s,
    box-shadow 0.3s;
}
.calm-trigger:hover {
  border-color: #d8d2c9;
  transform: none;
}
.calm-select.open .calm-trigger {
  border-color: #1f3b2c;
  box-shadow: 0 0 0 3px rgba(31, 59, 44, 0.08);
}
.calm-chevron {
  flex: 0 0 12px;
  width: 12px;
  height: 12px;
  color: #8d8a84;
  transition: transform 0.3s cubic-bezier(0.2, 0.8, 0.2, 1);
}
.calm-select.open .calm-chevron {
  transform: rotate(180deg);
}
.calm-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  left: 0;
  z-index: 45;
  margin: 0;
  padding: 6px;
  list-style: none;
  border: 1px solid #eae6df;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 18px 40px rgba(45, 42, 38, 0.13);
}
.calm-menu button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding: 10px 14px;
  border: 0;
  border-radius: 9px;
  background: transparent;
  color: #5f5c56;
  font-size: 14px;
  text-align: left;
  transition:
    background 0.2s,
    color 0.2s;
}
.calm-menu button:hover,
.calm-menu button:focus-visible {
  outline: none;
  border-color: transparent;
  background: rgba(31, 59, 44, 0.06);
  color: #1f3b2c;
  transform: none;
}
.calm-menu button.on {
  background: rgba(31, 59, 44, 0.1);
  color: #1f3b2c;
  font-weight: 600;
}
.calm-menu button svg {
  width: 14px;
  height: 14px;
  color: #1f3b2c;
}
.calm-pop-enter-active,
.calm-pop-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}
.calm-pop-enter-from,
.calm-pop-leave-to {
  opacity: 0;
  transform: translateY(-6px) scale(0.98);
}
</style>
