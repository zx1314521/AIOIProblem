<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { cpp } from '@codemirror/lang-cpp'
import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands'
import { EditorState } from '@codemirror/state'
import { EditorView, keymap, lineNumbers } from '@codemirror/view'

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const host = ref<HTMLElement | null>(null)
let view: EditorView | null = null
let syncing = false

onMounted(() => {
  if (!host.value) return
  view = new EditorView({
    parent: host.value,
    state: EditorState.create({
      doc: props.modelValue,
      extensions: [
        lineNumbers(),
        history(),
        cpp(),
        keymap.of([indentWithTab, ...defaultKeymap, ...historyKeymap]),
        EditorView.updateListener.of(update => {
          if (!update.docChanged || syncing) return
          emit('update:modelValue', update.state.doc.toString())
        }),
        EditorView.theme({
          '&': {
            height: '100%',
            minHeight: '0',
            background: '#101820',
            color: '#e7edf2',
            fontSize: '14px'
          },
          '.cm-scroller': {
            fontFamily: 'Consolas, "Cascadia Code", monospace',
            lineHeight: '1.58'
          },
          '.cm-content': {
            caretColor: '#e8f7ff'
          },
          '.cm-cursor, .cm-dropCursor': {
            borderLeftColor: '#e8f7ff'
          },
          '.cm-gutters': {
            background: '#0b1218',
            color: '#73808c',
            borderRight: '1px solid #26313b'
          },
          '.cm-activeLine': {
            background: '#17232d'
          },
          '.cm-activeLineGutter': {
            background: '#17232d',
            color: '#d8a94d'
          }
        })
      ]
    })
  })
})

watch(() => props.modelValue, value => {
  if (!view || value === view.state.doc.toString()) return
  syncing = true
  view.dispatch({
    changes: { from: 0, to: view.state.doc.length, insert: value }
  })
  syncing = false
})

onBeforeUnmount(() => {
  view?.destroy()
  view = null
})
</script>

<template>
  <div ref="host" class="code-editor" />
</template>

<style scoped>
.code-editor {
  min-height: 0;
  height: 100%;
  overflow: hidden;
  border: 1px solid #26313b;
  border-radius: 6px;
  background: #101820;
}
</style>
