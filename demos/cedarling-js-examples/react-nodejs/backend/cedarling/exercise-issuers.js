export async function exerciseIssuers(cedarling) {
  console.log('[exercise-issuers] starting');

  const result = await cedarling.issuers.isLoaded({ id: 'LocalMockIdP' });
  console.log(`[exercise-issuers] isLoaded("LocalMockIdP") ok=${result.ok} loaded=${result.ok ? result.value : result.error}`);

  const resultByIss = await cedarling.issuers.isLoaded({ iss: 'http://localhost:9090' });
  console.log(`[exercise-issuers] isLoaded(iss=http://localhost:9090) ok=${resultByIss.ok} loaded=${resultByIss.ok ? resultByIss.value : resultByIss.error}`);

  console.log('[exercise-issuers] done');
}
