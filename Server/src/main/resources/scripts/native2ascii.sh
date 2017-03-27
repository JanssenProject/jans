for filename in $1; do
    native2ascii -encoding utf8 $filename $filename
done