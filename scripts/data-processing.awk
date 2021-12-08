#!/usr/bin/awk -f
BEGIN{
    print "n,l,buildIndex,dbop,encIndex" > "enc-idx.csv"
    print "l,genToken" > "token-gen.csv"
    print "n,l,epsilon,numCandidates,filter,dbop,query" > "query.csv"
    print "l,genToken" > "token-gen-baseline.csv"
    print "n,l,epsilon,query" > "query-baseline.csv"
}
/DATA>> n /{
    n = $3
}
/DATA>> l /{
    l = $3
}
/DATA>> epsilon /{
    epsilon = $3
}
/DATA>> numCandidates /{
    numCandidates = $3
}
/DATA>> save/{
    acc+=$3
}
/DATA>> read/{
    acc+=$3
}
/DATA>> buildIndex /{
    buildIndex = $3
    acc = 0
}
/DATA>> encIndex /{
    printf "%d,%d,%d,%.0f,%.0f\n", n, l, buildIndex, acc, $3 >> "enc-idx.csv"
    acc = 0
}
/DATA>> genToken /{
    printf "%d,%d\n", l, $3 >> "token-gen.csv"
    acc = 0
}
/DATA>> filter /{
    filter = $3
}
/DATA>> query /{
    printf "%d,%d,%d,%d,%d,%.0f,%.0f\n", n, l, epsilon, numCandidates, filter, acc, $3 >> "query.csv"
    acc = 0
    filter = 0
}
/DATA>> genToken-baseline /{
    printf "%d,%d\n", l, $3 >> "token-gen-baseline.csv"
    acc = 0
}
/DATA>> query-baseline /{
    printf "%d,%d,%d,%.0f\n", n, l, epsilon, $3 >> "query-baseline.csv"
    acc = 0
}