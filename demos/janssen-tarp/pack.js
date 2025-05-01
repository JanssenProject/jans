const { readFileSync, existsSync, mkdirSync } = require('fs');
const { parse, resolve } = require('path');
const AdmZip = require('adm-zip');

try {
  const { base } = parse(__dirname);
  const { version } = JSON.parse(
    readFileSync(resolve(__dirname, 'dist/chrome', 'manifest.json'), 'utf8')
  );

  const outdir = 'release';
  const supportedBrowser = ['chrome', 'firefox']
  supportedBrowser.forEach(browser => {
    const filename = `${base}-${browser}-v${version}.zip`;
    const zip = new AdmZip();
    zip.addLocalFolder(`dist/${browser}`);
    if (!existsSync(outdir)) {
      mkdirSync(outdir);
    }
    zip.writeZip(`${outdir}/${filename}`);

    console.log(
      `Success! Created a ${filename} file under ${outdir} directory.`
    );

  })

} catch (e) {
  console.error('Error! Failed to generate a zip file.' + e);
}
