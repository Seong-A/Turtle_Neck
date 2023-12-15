package com.example.imageclassifier;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        ViewPager viewPager = findViewById(R.id.viewPager);
        InfoPagerAdapter adapter = new InfoPagerAdapter(getSupportFragmentManager(), viewPager);
        viewPager.setAdapter(adapter);
    }

    private class InfoPagerAdapter extends FragmentPagerAdapter {
        private ViewPager viewPager;

        public InfoPagerAdapter(FragmentManager fm, ViewPager viewPager) {
            super(fm);
            this.viewPager = viewPager;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new InfoFragment1(); //activity_info1
                case 1:
                    return new InfoFragment2(); //activity_info2
                case 2:
                    return new InfoFragment3(); //activity_info3
                case 3:
                    return new InfoFragment4(); //activity_info4
                case 4:
                    return new InfoFragment5(); //activity_info4


                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public void finishUpdate(ViewGroup container) {
            super.finishUpdate(container);

            // 페이지가 변경되었을 때 호출되는 메소드
            int currentItem = viewPager.getCurrentItem();

            // 마지막 페이지인 경우 SelectActivity로 이동
            if (currentItem == getCount() - 1) {
                // 마지막 페이지에서 오른쪽으로 슬라이딩하여 다음 액티비티로 이동
                Intent intent = new Intent(InfoActivity.this, SelectActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }
}
