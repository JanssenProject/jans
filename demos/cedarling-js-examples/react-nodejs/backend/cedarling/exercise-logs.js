export async function exerciseLogs(cedarling) {
  console.log('[exercise-logs] starting');

  const idsResult = await cedarling.logs.ids();
  console.log(`[exercise-logs] ids() ok=${idsResult.ok} count=${idsResult.ok ? idsResult.value.length : '(error)'}`);

  const findResult = await cedarling.logs.find();
  console.log(`[exercise-logs] find() ok=${findResult.ok} count=${findResult.ok ? findResult.value.length : '(error)'}`);

  const findWithTag = await cedarling.logs.find({ tag: 'decision' });
  console.log(`[exercise-logs] find(tag=decision) ok=${findWithTag.ok} count=${findWithTag.ok ? findWithTag.value.length : '(error)'}`);

  const drainResult = await cedarling.logs.drain();
  console.log(`[exercise-logs] drain() ok=${drainResult.ok} count=${drainResult.ok ? drainResult.value.length : '(error)'}`);

  console.log('[exercise-logs] done');
}
