<template>
  <form class="music-search-panel" @submit.prevent="$emit('search')">
    <div class="search-grid">
      <label>
        검색어
        <input v-model.trim="model.keyword" placeholder="제목 또는 아티스트" />
      </label>
      <label>
        장르
        <input v-model.trim="model.genre" placeholder="예: POP" />
      </label>
      <label>
        최소 BPM
        <input
          :value="model.minBpm"
          type="number"
          min="60"
          max="220"
          step="1"
          inputmode="numeric"
          @input="setBpm('minBpm', $event.target.value)"
        />
      </label>
      <label>
        최대 BPM
        <input
          :value="model.maxBpm"
          type="number"
          min="60"
          max="220"
          step="1"
          inputmode="numeric"
          @input="setBpm('maxBpm', $event.target.value)"
        />
      </label>
      <label>
        러닝 강도
        <select v-model="model.runningIntensity">
          <option value="">전체</option>
          <option value="LOW">LOW</option>
          <option value="MODERATE">MODERATE</option>
          <option value="HIGH">HIGH</option>
          <option value="INTERVAL">INTERVAL</option>
        </select>
      </label>
      <label>
        정렬
        <select v-model="model.sortType" @change="$emit('sort-change')">
          <option value="CREATED_DESC">최신순</option>
          <option value="TITLE_ASC">제목순</option>
          <option value="BPM_ASC">BPM 낮은순</option>
          <option value="BPM_DESC">BPM 높은순</option>
          <option value="RUNNING_FIT_DESC">러닝 적합도순</option>
        </select>
      </label>
    </div>
    <p v-if="validationMessage" class="field-error" role="alert">
      {{ validationMessage }}
    </p>
    <div class="form-actions">
      <button type="submit">조건 적용</button>
      <button type="button" class="secondary-button" @click="$emit('reset')">초기화</button>
    </div>
  </form>
</template>

<script setup>
const props = defineProps({
  model: { type: Object, required: true },
  validationMessage: { type: String, default: "" },
});

defineEmits(["search", "sort-change", "reset"]);

function setBpm(key, rawValue) {
  if (rawValue === "") {
    props.model[key] = "";
    return;
  }
  const parsed = Number(rawValue);
  props.model[key] = Number.isInteger(parsed) ? parsed : rawValue;
}
</script>
