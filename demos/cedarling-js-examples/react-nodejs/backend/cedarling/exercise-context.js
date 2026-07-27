export async function exerciseContext(cedarling) {
  console.log('[exercise-context] starting');

  const setResult = await cedarling.context.set('userId', 'bob', { ttlSeconds: 300 });
  console.log(`[exercise-context] set("userId", "bob") ok=${setResult.ok}`);

  const getResult = await cedarling.context.get('userId');
  console.log(`[exercise-context] get("userId") ok=${getResult.ok} value=${JSON.stringify(getResult.ok ? getResult.value : getResult.error)}`);

  const getEntryResult = await cedarling.context.getEntry('userId');
  console.log(`[exercise-context] getEntry("userId") ok=${getEntryResult.ok}`);

  const entriesResult = await cedarling.context.entries();
  console.log(`[exercise-context] entries() count=${entriesResult.ok ? entriesResult.value.length : '(error)'}`);

  const statsResult = await cedarling.context.stats();
  if (statsResult.ok) {
    const s = statsResult.value;
    console.log(`[exercise-context] stats: entryCount=${s.entryCount} maxEntries=${s.maxEntries} capacityUsagePercent=${s.capacityUsagePercent}`);
  }

  const deleteResult = await cedarling.context.delete('userId');
  console.log(`[exercise-context] delete("userId") ok=${deleteResult.ok}`);

  const clearResult = await cedarling.context.clear();
  console.log(`[exercise-context] clear() ok=${clearResult.ok}`);

  console.log('[exercise-context] done');
}
