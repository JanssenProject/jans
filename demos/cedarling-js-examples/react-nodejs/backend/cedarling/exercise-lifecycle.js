export async function exerciseLifecycle(cedarling) {
  console.log('[exercise-lifecycle] starting');

  const closeResult = await cedarling.close();
  console.log(`[exercise-lifecycle] close() ok=${closeResult.ok}`);

  console.log('[exercise-lifecycle] done');
}
