#include <stdlib.h>
#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <emmintrin.h>
#include <polyglot.h>

long numDigits(long n)
{
    long len = 0;
    while(n)
    {
        n=n/10;
        len++;
    }
    return len;
}

long vec_nle(__m128d *v, double f)
{
    return (v[0][0] <= f ||
        v[0][1] <= f ||
        v[1][0] <= f ||
        v[1][1] <= f ||
        v[2][0] <= f ||
        v[2][1] <= f ||
        v[3][0] <= f ||
        v[3][1] <= f) ? 0 : -1;
}

void clrPixels_nle(__m128d *v, double f, unsigned long * pix8)
{
    if(!(v[0][0] <= f)) *pix8 &= 0x7f;
    if(!(v[0][1] <= f)) *pix8 &= 0xbf;
    if(!(v[1][0] <= f)) *pix8 &= 0xdf;
    if(!(v[1][1] <= f)) *pix8 &= 0xef;
    if(!(v[2][0] <= f)) *pix8 &= 0xf7;
    if(!(v[2][1] <= f)) *pix8 &= 0xfb;
    if(!(v[3][0] <= f)) *pix8 &= 0xfd;
    if(!(v[3][1] <= f)) *pix8 &= 0xfe;
}

void calcSum(__m128d *r, __m128d *i, __m128d *sum, __m128d *init_r, __m128d init_i)
{
    for(long pair=0; pair<4; pair++)
    {
        __m128d r2 = r[pair] * r[pair];
        __m128d i2 = i[pair] * i[pair];
        __m128d ri = r[pair] * i[pair];

        sum[pair] = r2 + i2;

        r[pair]=r2 - i2 + init_r[pair];
        i[pair]=ri + ri + init_i;
    }
}

unsigned long mand8(__m128d *init_r, __m128d init_i)
{
    __m128d r[4], i[4], sum[4];
    for(long pair=0; pair<4; pair++)
    {
        r[pair]=init_r[pair];
        i[pair]=init_i;
    }

    unsigned long pix8 = 0xff;

    for (long j = 0; j < 6; j++)
    {
        for(long k=0; k<8; k++)
            calcSum(r, i, sum, init_r, init_i);

        if (vec_nle(sum, 4.0))
        {
            pix8 = 0x00;
            break;
        }
    }
    if (pix8)
    {
        calcSum(r, i, sum, init_r, init_i);
        calcSum(r, i, sum, init_r, init_i);
        clrPixels_nle(sum, 4.0, &pix8);
    }

    return pix8;
}

unsigned long mand64(__m128d *init_r, __m128d init_i)
{
    unsigned long pix64 = 0;

    for(long byte=0; byte<8; byte++)
    {
        unsigned long pix8 = mand8(init_r, init_i);

        pix64 = (pix64 >> 8) | (pix8 << 56);
        init_r += 4;
    }

    return pix64;
}

__m128d* simdAddDiv(__m128d* r0, long x) {
    return r0+x/2;
}

__m128d* SSEinit(long wid_ht) {
    __m128d* r0 = malloc(sizeof(__m128d)* wid_ht/2);
    for(long xy=0; xy<wid_ht; xy+=2)
    {
        r0[xy>>1] = 2.0 / wid_ht * (__m128d){xy,  xy+1} - 1.5;
    }

    return r0;
}

__m128d* genInitI(double d) {
    __m128d* initI = malloc(sizeof(__m128d));
    double arr[] = {d,d};
    *initI = _mm_load_pd(arr);
    return initI;
}


void initValues(double* i0, long wid_ht) {
    for(long xy=0; xy<wid_ht; xy+=2)
    {
        i0[xy]    = 2.0 / wid_ht *  xy    - 1.0;
        i0[xy+1]  = 2.0 / wid_ht * (xy+1) - 1.0;
    }
}

double blkhole;
double blackhole(double i) {
    blkhole = i;
    return blkhole;
}


void stepThroughValues(double* i0, long wid_ht) {
    for(long xy = 0; xy < wid_ht; xy++) {
        blkhole = i0[xy];
    }
}


//POLYGLOT_DECLARE_TYPE(double)

void initValuesN(long wid_ht) {
    double* i0 = malloc(sizeof(double) * wid_ht);

    for(long xy=0; xy<wid_ht; xy+=2) {
        i0[xy]    = 2.0 / wid_ht *  xy    - 1.0;
        i0[xy+1]  = 2.0 / wid_ht * (xy+1) - 1.0;
    }

    free(i0);
}

void incrementValues(double* i0, long wid_ht) {
    for(long xy=0; xy < wid_ht; xy++) {
        i0[xy] += 1;
    }
}

void releaseValuesN(double* i0) {
    free(i0);
}

void calcPixels(int x, __m128d* r0, double init_i, long* resArr) {
    double arr[] = {init_i,init_i};
    __m128d init = _mm_load_pd(arr);

    for(int i = 0; i < x; i++) {
       resArr[i] = mand64(r0+i*32, init);
    }

    return;
}

double init_i[2];
__m128d i0;

void loadInit(double d) {
    init_i[0] = d;
    init_i[1] = d;
    i0 = _mm_load_pd(init_i);
}

unsigned char calcPixels8(int x,  __m128d* r0) {
    return mand8(r0+x*4, i0);
}

void freem128d(__m128d* r0) {
    free(r0);
}

int mandelbrot(long wid_ht)
{
    // get width/height from arguments

    wid_ht = (wid_ht+7) & ~7;


    // allocate memory for header and pixels

    long headerLength = numDigits(wid_ht)*2+5;
    long pad = ((headerLength + 7) & ~7) - headerLength; // pad aligns pixels on 8
    long dataLength = headerLength + wid_ht*(wid_ht>>3);
    unsigned char * const buffer = malloc(pad + dataLength);
    unsigned char * const header = buffer + pad;
    unsigned char * const pixels = header + headerLength;


    // generate the bitmap header

    sprintf((char *)header, "P4\n%ld %ld\n", wid_ht, wid_ht);


    // calculate initial values, store in r0, i0

    __m128d r0[wid_ht/2];
    double i0[wid_ht];

    for(long xy=0; xy<wid_ht; xy+=2)
    {
        r0[xy>>1] = 2.0 / wid_ht * (__m128d){xy,  xy+1} - 1.5;
        i0[xy]    = 2.0 / wid_ht *  xy    - 1.0;
        i0[xy+1]  = 2.0 / wid_ht * (xy+1) - 1.0;
    }


    // generate the bitmap

    long use8 = wid_ht%64;
    if (use8)
    {
        // process 8 pixels (one byte) at a time
        #pragma omp parallel for schedule(guided)
        for(long y=0; y<wid_ht; y++)
        {
            __m128d init_i = (__m128d){i0[y], i0[y]};
            long rowstart = y*wid_ht/8;
            for(long x=0; x<wid_ht; x+=8)
            {
                pixels[rowstart + x/8] = mand8(r0+x/2, init_i);
            }
        }
    }
    else
    {
        // process 64 pixels (8 bytes) at a time
        #pragma omp parallel for schedule(guided)
        for(long y=0; y<wid_ht; y++)
        {
            __m128d init_i = (__m128d){i0[y], i0[y]};
            long rowstart = y*wid_ht/64;
            for(long x=0; x<wid_ht; x+=64)
            {
                ((unsigned long *)pixels)[rowstart + x/64] = mand64(r0+x/2, init_i);
            }
        }
    }

    // write the data

    int fd = open("/dev/null", O_WRONLY);
    long ret = ret = write(fd, header, dataLength);
    close(fd);


    free(buffer);

    return 0;
}